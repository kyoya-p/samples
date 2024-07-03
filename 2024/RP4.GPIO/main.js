const { requestGPIOAccess } = require("node-web-gpio");
const { promisify } = require("util");
const sleep = promisify(setTimeout);

async function main() {
  const gpioAccess = await requestGPIOAccess();
  const port = gpioAccess.ports.get(26);

  await port.export("out");

  for (; ;) {
    await port.write(1);
    console.log("26=1");
    await sleep(1000);
    await port.write(0);
    console.log("26=0");
    await sleep(1000);
  }
}

main();
