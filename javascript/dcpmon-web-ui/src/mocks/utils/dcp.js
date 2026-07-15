export async function getDcpByAddress(address) {
  try {
    // Dynamically import the JSON file based on the site name
    const data = await import(`../schema/dcp/${address?.toUpperCase()}.json`);
    return [data.default];
  } catch (error) {
    console.error(`Error loading data for site: ${address}`, error);
    return [];
  }
}
