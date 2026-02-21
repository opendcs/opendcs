import type { Meta, ReactRenderer, StoryObj } from "@storybook/react-vite";
import { fn } from "storybook/test";
import DecodesSample, { type DecodesSampleProperties } from "./Sample";
import { ApiDecodedMessage } from "opendcs-api";
import type { ArgsStoryFn } from "storybook/internal/csf";
import { useCallback } from "storybook/internal/preview-api";

export const testDataSets: {
  name: string;
  input: string;
  result: ApiDecodedMessage;
}[] = [
  {
    name: "Single Sensor",
    input: "25.5 26.6",
    result: {
      messageTime: new Date(2025, 1, 19, 10, 0, 0, 0),
      timeSeries: [
        {
          sensorNum: 1,
          sensorName: "Stage",
          units: "ft",
          values: [
            {
              time: new Date(2025, 1, 19, 10, 0, 0, 0),
              rawDataPosition: { start: 0, end: 4 },
              value: "25.5",
            },
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 5, end: 8 },
              value: "26.6",
            },
          ],
        },
      ],
    },
  },
  {
    name: "Two Sensors Matching times",
    input: "25.5 26.6\n13.3 14.4",
    result: {
      messageTime: new Date(2025, 1, 19, 10, 0, 0, 0),
      timeSeries: [
        {
          sensorNum: 1,
          sensorName: "Stage",
          units: "ft",
          values: [
            {
              time: new Date(2025, 1, 19, 10, 0, 0, 0),
              rawDataPosition: { start: 0, end: 4 },
              value: "25.5",
            },
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 5, end: 8 },
              value: "26.6",
            },
          ],
        },
        {
          sensorNum: 2,
          sensorName: "Precip",
          units: "in",
          values: [
            {
              time: new Date(2025, 1, 19, 10, 0, 0, 0),
              rawDataPosition: { start: 10, end: 14 },
              value: "13.3",
            },
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 15, end: 18 },
              value: "14.4",
            },
          ],
        },
      ],
    },
  },
  {
    name: "Three Sensors Non-Matching times",
    input: "25.5 26.6\n13.3 14.4\n5.5",
    result: {
      messageTime: new Date(2025, 1, 19, 10, 0, 0, 0),
      timeSeries: [
        {
          sensorNum: 1,
          sensorName: "Stage",
          units: "ft",
          values: [
            {
              time: new Date(2025, 1, 19, 10, 0, 0, 0),
              rawDataPosition: { start: 0, end: 4 },
              value: "25.5",
            },
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 5, end: 8 },
              value: "26.6",
            },
          ],
        },
        {
          sensorNum: 2,
          sensorName: "Precip",
          units: "in",
          values: [
            {
              time: new Date(2025, 1, 19, 10, 0, 0, 0),
              rawDataPosition: { start: 10, end: 14 },
              value: "13.3",
            },
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 15, end: 18 },
              value: "14.4",
            },
          ],
        },
        {
          sensorNum: 3,
          sensorName: "Battery",
          units: "v",
          values: [
            {
              time: new Date(2025, 1, 19, 11, 0, 0, 0),
              rawDataPosition: { start: 19, end: 21 },
              value: "5.5",
            },
          ],
        },
      ],
    },
  },
];

export const decodeData = (raw: string): ApiDecodedMessage => {
  return testDataSets.find((tds) => tds.input === raw)?.result || {};
};

const WithDecodedMessage: ArgsStoryFn<ReactRenderer, DecodesSampleProperties> = (
  args,
) => {
  const decodeData = useCallback((raw: string): ApiDecodedMessage => {
    args.decodeData?.(raw);
    return decodeData(raw);
  }, []);

  return (
    <>
      Paste desired test data into text area.
      <ul>
        {testDataSets.map((tds) => {
          return (
            <li key={tds.name}>
              {tds.name}:{" "}
              <pre style={{ border: "thin solid black" }}>
                <code>{tds.input}</code>
              </pre>
            </li>
          );
        })}
      </ul>
      <DecodesSample decodeData={decodeData} />
    </>
  );
};

const meta = {
  component: DecodesSample,
} satisfies Meta<typeof DecodesSample>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    decodeData: fn((raw: string): ApiDecodedMessage => {
      console.log(raw);
      return {};
    }),
  },
  render: WithDecodedMessage,
  play: async ({ mount }) => {
    const _canvas = await mount();
  },
};
