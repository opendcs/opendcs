import { fireEvent, render, screen } from '@testing-library/react';
 
// Replace your-framework with the framework you are using, e.g. react-vite, nextjs, nextjs-vite, etc.
import { composeStories } from '@storybook/react-vite';


import * as stories from './Properties.stories';
import {expect, test} from 'vitest';


const { Empty } = composeStories(stories);

test(' can add then remove prop', async () => {
    await Empty.run();
    const add = screen.getByRole('button', {name: 'add property'});
    fireEvent.click(add);
    expect(Empty.args.theProps?.length).toBe(1);
});

