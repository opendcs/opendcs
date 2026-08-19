import { Decorator } from "@storybook/react-vite";
import { SiteNameTypeProvider } from "../../src/contexts/app/SiteNameTypeProvider";

export const WithSiteNameType: Decorator = (Story) => (
  <SiteNameTypeProvider>
    <Story />
  </SiteNameTypeProvider>
);
