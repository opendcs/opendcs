package org.opendcs.lrgs.http.dds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.opendcs.lrgs.http.dto.DcpIdentifier;
import org.opendcs.lrgs.http.dto.DataGroup;
import org.opendcs.lrgs.http.dto.DcpSummary;
import org.opendcs.lrgs.http.dto.GoesMessage;
import org.opendcs.lrgs.http.dto.StatusCounts;
import org.opendcs.lrgs.http.dto.StatusGroupSummary;
import org.opendcs.lrgs.messages.MessageRetrieval;

import decodes.util.Pdt;
import decodes.util.PdtEntry;
import ilex.util.EnvExpander;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpMsgRetriever;
import lrgs.common.NetworkList;
import lrgs.common.NetworkListItem;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchSyntaxException;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsMain;

final class DdsSummaryService
{
    static final int DURATION_HOURS = 24;

    private DdsSummaryService()
    {
    }

    static StatusGroupSummary summarize(DcpMsgRetriever retriever, LrgsMain lrgs, String group)
        throws IOException, SearchSyntaxException, ArchiveUnavailableException
    {
        NetworkList netlist = loadNetlist(group);
        SearchCriteria criteria = new SearchCriteria();
        criteria.clear();
        criteria.setLrgsSince("now - " + DURATION_HOURS + " hours");
        criteria.setAscendingTimeOnly(true);
        for (NetworkListItem item : netlist)
            criteria.ExplicitDcpAddrs.add(item.addr);
        retriever.setSearchCriteria(criteria);

        var result = MessageRetrieval.getMessages(retriever, lrgs, Integer.MAX_VALUE);
        if (result.ex() != null)
            result = MessageRetrieval.getMessages(retriever, lrgs, Integer.MAX_VALUE);
        List<GoesMessage> messages = result.messages();
        Map<String, List<GoesMessage>> byAddress = new LinkedHashMap<>();
        for (GoesMessage message : messages)
        {
            String address = addressFrom(message);
            byAddress.computeIfAbsent(address, ignored -> new ArrayList<>()).add(message);
        }

        int missing = 0, partial = 0, parity = 0, complete = 0, unknown = 0;
        Map<String, DcpSummary> summaries = new LinkedHashMap<>();
        for (NetworkListItem item : netlist)
        {
            String address = item.addr.toString();
            List<GoesMessage> dcpMessages = byAddress.getOrDefault(address, List.of());
            int messageTotal = (int)dcpMessages.stream().filter(m -> "g-s-t".equals(m.cType())).count();
            int parityCount = (int)dcpMessages.stream().filter(DdsSummaryService::hasParity).count();
            boolean lowBattery = dcpMessages.stream().anyMatch(DdsSummaryService::hasLowBattery);
            Integer expected = expectedMessages(Pdt.instance().find(item.addr));
            String status;
            if (expected == null)
            {
                status = "unknown";
                unknown++;
            }
            else if (messageTotal == 0)
            {
                status = "missing";
                missing++;
            }
            else if (messageTotal < expected)
            {
                status = "partial";
                partial++;
            }
            else if (parityCount > 0)
            {
                status = "parity";
                parity++;
            }
            else
            {
                status = "complete";
                complete++;
            }

            List<DcpIdentifier> identifiers = new ArrayList<>();
            identifiers.add(new DcpIdentifier("NESDIS", address));
            if (item.name != null && !item.name.isBlank())
                identifiers.add(new DcpIdentifier("Local", item.name));
            summaries.put(address, new DcpSummary(
                identifiers, status, messageTotal, expected, parityCount, lowBattery));
        }

        return new StatusGroupSummary(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC"))),
            DURATION_HOURS,
            new StatusCounts(missing, partial, parity, complete, unknown), summaries);
    }

    static List<DataGroup> listGroups() throws IOException
    {
        Path directory = netlistDirectory();
        if (!Files.isDirectory(directory))
            return List.of();

        try (var files = Files.list(directory))
        {
            return files
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".nl"))
                .map(name -> name.substring(0, name.length() - 3))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(name -> new DataGroup(name, name))
                .toList();
        }
    }

    private static NetworkList loadNetlist(String group) throws IOException
    {
        if (group == null || group.isBlank() || group.contains("/") || group.contains("\\"))
            throw new IOException("Invalid data group");
        Path directory = netlistDirectory();
        Path file = directory.resolve(group);
        if (!Files.isRegularFile(file))
            file = directory.resolve(group + ".nl");
        if (!Files.isRegularFile(file))
        {
            try (var files = Files.list(directory))
            {
                String requested = group.toLowerCase(java.util.Locale.ROOT);
                file = files
                    .filter(Files::isRegularFile)
                    .filter(candidate -> {
                        String name = candidate.getFileName().toString();
                        String withoutExtension = name.toLowerCase(java.util.Locale.ROOT).endsWith(".nl")
                            ? name.substring(0, name.length() - 3) : name;
                        return withoutExtension.toLowerCase(java.util.Locale.ROOT).equals(requested);
                    })
                    .findFirst()
                    .orElse(null);
            }
        }
        if (file == null || !Files.isRegularFile(file))
            throw new IOException("No such data group '" + group + "'");
        return new NetworkList(file.toFile());
    }

    private static Path netlistDirectory()
    {
        return new File(EnvExpander.expand(LrgsConfig.instance().ddsNetlistDir)).toPath();
    }

    private static String addressFrom(GoesMessage message)
    {
        return message.dcpAddress();
    }

    private static boolean hasParity(GoesMessage message)
    {
        return "?".equals(message.arm()) || message.data().contains(" M ");
    }

    private static boolean hasLowBattery(GoesMessage message)
    {
        return "V".equals(message.arm()) || message.data().contains(" V ");
    }

    private static Integer expectedMessages(PdtEntry entry)
    {
        if (entry == null || entry.st_xmit_interval <= 0)
            return null;
        int durationSeconds = DURATION_HOURS * 60 * 60;
        return (durationSeconds + entry.st_xmit_interval - 1) / entry.st_xmit_interval;
    }
}
