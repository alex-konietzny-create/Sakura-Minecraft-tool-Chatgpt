package de.txtmanager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class PackMixer {
    private PackMixer() {}

    public static List<String> listPacks(Path resourcepacks) throws IOException {
        if (!Files.isDirectory(resourcepacks)) return List.of();
        try (var stream = Files.list(resourcepacks)) {
            return stream.filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    public static List<String> listTextures(Path resourcepacks, String packName) throws IOException {
        Path pack = safePack(resourcepacks, packName);
        List<String> result = new ArrayList<>();
        if (Files.isDirectory(pack)) {
            try (var stream = Files.walk(pack)) {
                stream.filter(Files::isRegularFile).forEach(p -> {
                    String relative = pack.relativize(p).toString().replace('\\', '/');
                    if (isTexture(relative)) result.add(relative);
                });
            }
        } else {
            try (ZipFile zip = new ZipFile(pack.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.isDirectory() && isTexture(entry.getName())) result.add(entry.getName());
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    public static void export(Path resourcepacks, Map<String, String> selections, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), "txt-mix-", ".zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                write(zip, "pack.mcmeta", "{\"pack\":{\"pack_format\":75,\"description\":\"TXT Texture Manager Mix\"}}".getBytes(StandardCharsets.UTF_8));
                for (var selected : new TreeMap<>(selections).entrySet()) {
                    String path = selected.getKey();
                    if (!isTexture(path)) throw new IOException("Invalid texture path: " + path);
                    Path pack = safePack(resourcepacks, selected.getValue());
                    byte[] bytes;
                    if (Files.isDirectory(pack)) {
                        Path source = pack.resolve(path).normalize();
                        if (!source.startsWith(pack)) throw new IOException("Unsafe path");
                        bytes = Files.readAllBytes(source);
                    } else {
                        try (ZipFile sourceZip = new ZipFile(pack.toFile())) {
                            ZipEntry entry = sourceZip.getEntry(path);
                            if (entry == null) throw new IOException("Missing texture: " + path);
                            try (InputStream input = sourceZip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                        }
                    }
                    write(zip, path, bytes);
                }
            }
            try { Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static Path safePack(Path root, String name) throws IOException {
        if (name.isBlank() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) throw new IOException("Invalid pack name");
        Path pack = root.toAbsolutePath().normalize().resolve(name).normalize();
        if (!pack.startsWith(root.toAbsolutePath().normalize()) || !Files.exists(pack)) throw new IOException("Pack not found: " + name);
        return pack;
    }
    private static boolean validPath(String path) { return !path.startsWith("/") && !path.contains("\\") && Arrays.stream(path.split("/")).noneMatch(s -> s.equals("..") || s.equals(".")); }
    private static boolean isTexture(String path) { return path.startsWith("assets/") && path.contains("/textures/") && path.endsWith(".png") && validPath(path); }
    private static void write(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path)); zip.write(bytes); zip.closeEntry();
    }
}
