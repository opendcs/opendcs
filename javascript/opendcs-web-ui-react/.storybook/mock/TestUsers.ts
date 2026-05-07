import { User } from "opendcs-api";

export const BasicUser: User = {
  id: { value: 1 },
  email: "BasicUser@noreply.com",
  createdAt: new Date("2026-05-06T00:00:00Z"),
  updatedAt: new Date("2026-05-07T00:00:00Z"),
  roles: [
    { id: { value: 1 }, name: "TestRole", updatedAt: new Date("2026-05-01T00:00:00Z") },
  ],
  identityProviders: [
    {
      provider: { id: { value: 1 }, name: "builtin", type: "BuiltIn" },
      subject: "BasicUser@noreply.com",
    },
  ],
};
