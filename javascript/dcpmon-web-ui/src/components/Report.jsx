import { useEffect, useState } from "react";
import { LRGS_DOMAIN } from "../constants";

export default function Report() {
  const [addresses, setAddresses] = useState([]);

  useEffect(() => {
    fetch(`${LRGS_DOMAIN}/dcp/KEYS`)
      .then((res) => res.json())
      .then(setAddresses);
  }, []);
  console.log(addresses);
  return (
    <div>
      <ul>
        {addresses &&
          addresses.map((addr) => (
            <li key={addr.timestamp}>{addr.timestamp}</li>
          ))}
      </ul>
    </div>
  );
}
