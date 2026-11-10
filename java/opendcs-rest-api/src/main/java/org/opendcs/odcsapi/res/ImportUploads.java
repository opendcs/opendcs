/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.opendcs.odcsapi.beans.ApiImportResult;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/** Keeps pending import files until confirmation or session invalidation. */
@WebListener
public final class ImportUploads implements ServletContextListener
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String SESSION_KEY = ImportUploads.class.getName();
    private static final String DIRECTORY_KEY = SESSION_KEY + ".directory";

    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        ServletContext context = event.getServletContext();
        File tempDirectory = (File) context.getAttribute(ServletContext.TEMPDIR);
        Path directory = tempDirectory.toPath().resolve("opendcs-imports");
        try
        {
            if (directory.getFileSystem().supportedFileAttributeViews().contains("posix"))
            {
                Files.createDirectories(directory,
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            }
            else
            {
                Files.createDirectories(directory);
            }
            cleanup(directory);
            context.setAttribute(DIRECTORY_KEY, directory);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to initialize import upload directory.", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
        Path directory = (Path) event.getServletContext().getAttribute(DIRECTORY_KEY);
        if (directory != null)
        {
            try
            {
                cleanup(directory);
            }
            catch (IOException ex)
            {
                log.warn("Unable to clean import uploads at shutdown.", ex);
            }
        }
    }

    private static void cleanup(Path directory) throws IOException
    {
        try (var files = Files.newDirectoryStream(directory, "*.upload"))
        {
            for (Path file : files)
            {
                Files.deleteIfExists(file);
            }
        }
    }

    static String stage(HttpSession session, String organization, String content, ApiImportResult preview)
            throws WebAppException
    {
        Path directory = (Path) session.getServletContext().getAttribute(DIRECTORY_KEY);
        Path file = null;
        boolean retained = false;
        try
        {
            // Example staged filename: import-1234567890123456789.upload
            file = Files.createTempFile(directory, "import-", ".upload");
            Files.writeString(file, content);
            String id = UUID.randomUUID().toString();
            synchronized (session)
            {
                PendingUploads pending = (PendingUploads) session.getAttribute(SESSION_KEY);
                if (pending == null)
                {
                    pending = new PendingUploads();
                    session.setAttribute(SESSION_KEY, pending);
                }
                pending.add(id, new Upload(file, organization, preview));
            }
            retained = true;
            return id;
        }
        catch (IllegalStateException ex)
        {
            throw new WebAppException(401, "Session expired while staging the import. Upload the file again.", ex);
        }
        catch (IOException ex)
        {
            throw new WebAppException(500, "Unable to stage the uploaded file.", ex);
        }
        finally
        {
            if (!retained && file != null)
            {
                delete(file);
            }
        }
    }

    static Upload take(HttpSession session, String id, String organization) throws WebAppException
    {
        if (session != null)
        {
            try
            {
                PendingUploads pending = (PendingUploads) session.getAttribute(SESSION_KEY);
                if (pending != null)
                {
                    Upload upload = pending.take(id, organization);
                    if (upload != null)
                    {
                        return upload;
                    }
                }
            }
            catch (IllegalStateException ex)
            {
                throw new WebAppException(401, "Session expired. Upload the file again.", ex);
            }
        }
        throw new WebAppException(404, "Upload is unavailable in this session or organization. Preview the file again.");
    }

    private static void delete(Path file)
    {
        try
        {
            Files.deleteIfExists(file);
        }
        catch (IOException ex)
        {
            log.warn("Unable to delete import upload {}", file, ex);
        }
    }

    record Upload(Path file, String organization, ApiImportResult preview) implements AutoCloseable
    {
        @Override
        public void close()
        {
            delete(file);
        }
    }

    private static final class PendingUploads implements HttpSessionBindingListener
    {
        private final Map<String, Upload> uploads = new HashMap<>();
        private boolean closed;

        synchronized void add(String id, Upload upload)
        {
            if (closed)
            {
                throw new IllegalStateException("Session upload storage is closed.");
            }
            uploads.put(id, upload);
        }

        synchronized Upload take(String id, String organization)
        {
            Upload upload = uploads.get(id);
            if (upload != null && Objects.equals(organization, upload.organization()))
            {
                uploads.remove(id);
                return upload;
            }
            return null;
        }

        @Override
        public synchronized void valueUnbound(HttpSessionBindingEvent event)
        {
            closed = true;
            uploads.values().forEach(Upload::close);
            uploads.clear();
        }
    }
}
