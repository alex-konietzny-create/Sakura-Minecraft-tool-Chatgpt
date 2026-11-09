package de.txtmanager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class TextureManagerScreen extends Screen {
    private final Screen parent;
    private final Map<String, String> selections = new LinkedHashMap<>();
    private List<String> packs = List.of(), textures = List.of();
    private int packIndex = 0, textureIndex = 0;
    private String status = "Select a pack and texture";
    private final Path resourcepacks = MinecraftClient.getInstance().runDirectory.toPath().resolve("resourcepacks");

    public TextureManagerScreen(Screen parent) { super(Text.literal("TXT Texture Manager")); this.parent = parent; }

    @Override protected void init() {
        try { packs = PackMixer.listPacks(resourcepacks); refreshTextures(); }
        catch (IOException e) { status = e.getMessage(); }
        int x = width / 2 - 125, y = height / 2 - 70;
        addDrawableChild(ButtonWidget.builder(Text.literal("< Pack"), b -> { packIndex = Math.floorMod(packIndex - 1, Math.max(1,packs.size())); refreshTextures(); }).dimensions(x, y, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pack >"), b -> { packIndex = Math.floorMod(packIndex + 1, Math.max(1,packs.size())); refreshTextures(); }).dimensions(x+135, y, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("< Texture"), b -> { textureIndex = Math.floorMod(textureIndex - 1, Math.max(1,textures.size())); }).dimensions(x, y+50, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Texture >"), b -> { textureIndex = Math.floorMod(textureIndex + 1, Math.max(1,textures.size())); }).dimensions(x+135, y+50, 115, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Add selected texture"), b -> select()).dimensions(x, y+80, 250, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Export TXT-Mix.zip"), b -> export()).dimensions(x, y+110, 250, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(x, y+140, 250, 20).build());
    }
    private void refreshTextures() {
        textureIndex = 0;
        if (packs.isEmpty()) { textures = List.of(); status = "Put ZIP packs into resourcepacks"; return; }
        try { textures = PackMixer.listTextures(resourcepacks, packs.get(packIndex)); status = textures.size() + " textures found"; }
        catch (IOException e) { textures = List.of(); status = e.getMessage(); }
    }
    private void select() {
        if (packs.isEmpty() || textures.isEmpty()) return;
        selections.put(textures.get(textureIndex), packs.get(packIndex));
        status = selections.size() + " textures selected";
    }
    private void export() {
        if (selections.isEmpty()) { status = "Select at least one texture first"; return; }
        try { PackMixer.export(resourcepacks, selections, resourcepacks.resolve("TXT-Mix.zip")); status = "Exported! Enable TXT-Mix in Resource Packs"; }
        catch (IOException e) { status = "Export failed: " + e.getMessage(); }
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int x = width/2, y = height/2 - 90;
        context.drawCenteredTextWithShadow(textRenderer, "TXT Texture Manager", x, y-12, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, packs.isEmpty() ? "No packs" : truncate(packs.get(packIndex)), x, y+35, 0xFFFFAA);
        context.drawCenteredTextWithShadow(textRenderer, textures.isEmpty() ? "No textures" : truncate(textures.get(textureIndex)), x, y+85, 0xAAFFFF);
        context.drawCenteredTextWithShadow(textRenderer, truncate(status), x, y+240, 0xFFFFFF);
    }
    private static String truncate(String s) { return s.length() > 55 ? "..." + s.substring(s.length()-52) : s; }
    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
}
