import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { render, screen, waitFor } from "@testing-library/react";
import Dashboard from "../src/components/Dashboard";

const server = setupServer(
  // Describe network behavior with request handlers.
  // Tip: move the handlers into their own module and
  // import it across your browser and Node.js setups!
  http.get("/posts", ({ request, params, cookies }) => {
    return HttpResponse.json([
      {
        id: "f8dd058f-9006-4174-8d49-e3086bc39c21",
        title: `Avoid Nesting When You're Testing`,
      },
      {
        id: "8ac96078-6434-4959-80ed-cc834e7fef61",
        title: `How I Built A Modern Website In 2021`,
      },
    ]);
  })
);

// Enable request interception.
beforeAll(() => server.listen());

// Reset handlers so that each test could alter them
// without affecting other, unrelated tests.
afterEach(() => server.resetHandlers());

// Don't forget to clean up afterwards.
afterAll(() => server.close());

it("displays the list of recent posts", async () => {
  render(<Dashboard />);

  // 🕗 Wait for the posts request to be finished.
  await waitFor(() => {
    expect(
      screen.getByLabelText("Fetching latest posts...")
    ).not.toBeInTheDocument();
  });

  // ✅ Assert that the correct posts have loaded.
  expect(
    screen.getByRole("link", { name: /Avoid Nesting When You're Testing/ })
  ).toBeVisible();

  expect(
    screen.getByRole("link", { name: /How I Built A Modern Website In 2021/ })
  ).toBeVisible();
});
