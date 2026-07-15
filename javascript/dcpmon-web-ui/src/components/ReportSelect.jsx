import InputRadio from "./forms/InputRadio";
import { useQuery } from "@tanstack/react-query";
import { LRGS_DOMAIN } from "../constants";

export default function ReportSelect({ setForm }) {
  const groupData = useQuery({
    queryKey: ["groups"],
    queryFn: async () => {
      return fetch(`${LRGS_DOMAIN}/groups`).then((res) => res.json());
    },
  });

  const channelData = useQuery({
    queryKey: ["channel"],
    queryFn: async () => {
      // TODO: Channel is specific to GOES, but what about IRRIDIUM
      return fetch(`${LRGS_DOMAIN}/channel`).then((res) => res.json());
    },
    select: (data) => {
      return data?.channels;
    },
  });

  return (
    <div className="max-w-4xl mx-auto px-4 py-6">
      <h1 className="text-2xl font-bold mb-6">Report Select</h1>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-1/4">
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="DCP Group"
            formId="group_select"
          />
        </div>
        <div className="flex-1">
          <div className="relative">
            <select className="w-full p-2 border border-gray-300 rounded">
              <option>Select Group</option>
              {groupData.data?.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.id}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-1/4">
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="Channel"
            formId="channel_select"
          />
        </div>
        <div className="flex-1">
          <div className="relative">
            <select className="w-full p-2 border border-gray-300 rounded">
              <option>Select Channel</option>
              {channelData.data?.map((channel, idx) => (
                <option key={channel + "_" + idx} value={channel}>
                  {channel}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-1/4">
          <InputRadio
            onChange={setForm}
            group="report_text"
            text="Enter DCP name/address"
            formId="dcp_text"
          />
        </div>
        <div className="flex-1">
          <input
            type="text"
            placeholder="CE609B50"
            aria-label="DCP Name/Address"
            className="w-full p-2 border border-gray-300 rounded"
          />
        </div>
      </div>
    </div>
  );
}
