import type { Preview } from '@storybook/react-vite'
//import { useArgs } from '@storybook/preview-api';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'datatables.net-bs5'
import 'datatables.net-responsive-bs5'
import '../src/main.css'
import '../src/assets/opendcs-shim.css'
import { useArgs } from 'storybook/internal/preview-api';


const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
       color: /(background|color)$/i,
       date: /Date$/i,
      },
    },

    a11y: {
      // 'todo' - show a11y violations in the test UI only
      // 'error' - fail CI on a11y violations
      // 'off' - skip a11y checks entirely
      test: 'todo'
    }
  },
  decorators: [
    (story, context) => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const [_, updateArgs] = useArgs();
      return story({ ...context, updateArgs });
    },
  ],
};

export default preview;