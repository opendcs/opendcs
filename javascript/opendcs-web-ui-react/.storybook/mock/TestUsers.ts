import { OpenDcsApiRole, OpenDcsPrincipal } from "opendcs-api";

const USER: OpenDcsApiRole = { role: "ODCS_API_USER", description: "Generic User" };
const ADMIN: OpenDcsApiRole = {
  role: "ODCS_API_ADMIN",
  description: "User with addition privleges",
};

export const BasicUser: OpenDcsPrincipal = {
  id: { value: 1 },
  email: "BasicUser@noreply.com",
  createdAt: new Date("2026-05-06T00:00:00Z"),
  updatedAt: new Date("2026-05-07T00:00:00Z"),
  roles: {
    "": [USER],
  },
  identityProviders: [
    {
      provider: { id: { value: 1 }, name: "builtin", type: "BuiltIn" },
      subject: "BasicUser@noreply.com",
    },
  ],
};

export const CwmsUser: OpenDcsPrincipal = {
  ...BasicUser,
  roles: {
    SPK: [USER, ADMIN],
    SWT: [USER],
  },
};
