import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) throws Exception {
        // 数値型(long/int)に格納せず、Durationオブジェクトとして管理
        Duration interval = Duration.ofSeconds(args.length > 0 ? Long.parseLong(args[0]) : 10L);
        Duration totalDuration = Duration.ofSeconds(args.length > 1 ? Long.parseLong(args[1]) : 60L);
        int maxImages = args.length > 2 ? Integer.parseInt(args[2]) : 100;
        
        Path outputDir = Paths.get(".screenshot");
        Path ffmpegPath = Paths.get("../ffmpeg/bin/ffmpeg.exe");

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        if (!Files.exists(ffmpegPath)) {
            System.err.println("Error: ffmpeg not found at " + ffmpegPath);
            return;
        }

        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(totalDuration);

        System.out.println("--- ScreenCapt Logger (Java stdlib only) ---");
        System.out.println("Interval: " + interval + ", Total Duration: " + totalDuration + ", Max Images: " + maxImages);
        System.out.println("Start Time: " + ZonedDateTime.now());
        System.out.println("End Time  : " + ZonedDateTime.ofInstant(endTime, ZonedDateTime.now().getZone()));

        while (Instant.now().isBefore(endTime)) {
            try {
                LocalDateTime now = LocalDateTime.now();
                // 数値コンポーネントを取り出さず、toString()からタイムスタンプ生成
                String timestamp = now.toString()
                    .split("\\.")[0]
                    .replace("-", "")
                    .replace(":", "")
                    .replace("T", "-");
                
                Path outputFile = outputDir.resolve("screenshot-" + timestamp + ".jpg");
                
                captureWithFfmpeg(ffmpegPath, outputFile);
                cleanupOldImages(outputDir, maxImages);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            
            Duration remaining = Duration.between(Instant.now(), endTime);
            if (remaining.compareTo(interval) > 0) {
                Thread.sleep(interval.toMillis());
            } else if (!remaining.isNegative()) {
                Thread.sleep(remaining.toMillis());
            }
        }
        
        System.out.println("ScreenCapt finished (Total duration " + totalDuration + " elapsed).");
    }

    private static void captureWithFfmpeg(Path ffmpeg, Path output) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            ffmpeg.toString(),
            "-y",
            "-f", "gdigrab",
            "-i", "desktop",
            "-frames:v", "1",
            output.toString()
        );
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        
        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        
        if (finished && process.exitValue() == 0) {
            System.out.println("[" + LocalDateTime.now() + "] Captured: " + output.getFileName());
        } else {
            process.destroyForcibly();
            System.err.println("FFmpeg capture failed");
        }
    }

    private static void cleanupOldImages(Path outputDir, int maxImages) throws Exception {
        List<Path> images;
        try (java.util.stream.Stream<Path> stream = Files.list(outputDir)) {
            images = stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith("screenshot-") && name.endsWith(".jpg");
            })
            .sorted(Comparator.comparing((Path p) -> {
                try {
                    return Files.getLastModifiedTime(p).toInstant();
                } catch (Exception e) {
                    return Instant.MIN;
                }
            }).reversed())
            .collect(Collectors.toList());
        }
            
        if (images.size() > maxImages) {
            for (int i = maxImages; i < images.size(); i++) {
                Files.deleteIfExists(images.get(i));
                System.out.println("Deleted old image: " + images.get(i).getFileName());
            }
        }
    }
}
