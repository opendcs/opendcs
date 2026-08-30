import { GoesMessageToJSON, type GoesMessage } from "opendcs-dds-api";

const messages: GoesMessage[] = [
  {
    messageType: "GOES",
    dcpAddress: "CE1F40D4",
    receiveTime: new Date("2025-06-22T20:03:01Z"),
    dataSource: { name: "GOES", type: "GOES" },
    cType: "g-s-t",
    arm: "G",
    eirp: "41",
    frequency: "+0",
    modulation: "N",
    quality: "N",
    channel: "162W",
    data: ":HP 5 #30 749.73 749.74 749.75 749.76 :HT 9 #30 649.67 649.66 649.66 649.67 :PC 3 #60 22.67 22.67",
  },
  {
    messageType: "GOES",
    dcpAddress: "CE1F40D4",
    receiveTime: new Date("2025-06-22T21:03:01Z"),
    dataSource: { name: "GOES", type: "GOES" },
    arm: "G",
    eirp: "42",
    frequency: "+0",
    modulation: "N",
    quality: "N",
    channel: "162W",
    data: ":HP 5 #30 749.71 749.71 749.73 749.74 :HT 9 #30 649.65 649.66 649.67 649.66 :PC 3 #60 22.67 22.67",
  },
];

export const goesMessages = {
  total: 2,
  messages: messages.map(GoesMessageToJSON),
};
