// ヘッドフルでなければ動作しない模様

const { launch, getStream } = require("puppeteer-stream");
const fs = require("fs");

const file = fs.createWriteStream("test.webm");

async function test() {
	const browser = await launch({
        headless: 'new',
		defaultViewport: {
			width: 1920,
			height: 1080,
		},
	});

	const page = await browser.newPage();
    await page.goto("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
    console.log("opened URL");
    const stream = await getStream(page, { audio: true, video: true });
	console.log("recording");

	stream.pipe(file);
	setTimeout(async () => {
		await stream.destroy();
		file.close();
		console.log("finished");
	}, 1000 * 10);
}

test();
