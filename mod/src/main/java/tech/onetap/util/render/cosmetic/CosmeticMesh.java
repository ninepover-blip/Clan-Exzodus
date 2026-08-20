package tech.onetap.util.render.cosmetic;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Lightweight reader for the IFM1/IFM2 meshes generated from the supplied Warcraft MDX models. */
public final class CosmeticMesh {
    private static final int MAGIC_IFM1 = 0x49464D31; // IFM1
    private static final int MAGIC_IFM2 = 0x49464D32; // IFM2 (with per-vertex colors)
    private static final int MAX_VERTICES = 100_000;
    private static final int MAX_INDICES = 150_000;
    private static final Map<String, CosmeticMesh> CACHE = new HashMap<>();
    private static final CosmeticMesh EMPTY = new CosmeticMesh(new float[0], new float[0], new int[0]);

    private final float[] vertices;
    private final float[] colors;
    private final int[] indices;

    private CosmeticMesh(float[] vertices, float[] colors, int[] indices) {
        this.vertices = vertices;
        this.colors = colors;
        this.indices = indices;
    }

    public static CosmeticMesh get(String name) {
        return CACHE.computeIfAbsent(name, CosmeticMesh::load);
    }

    private static CosmeticMesh load(String name) {
        String path = "/assets/mre/cosmetics/meshes/" + name + ".mesh";
        try (InputStream raw = CosmeticMesh.class.getResourceAsStream(path)) {
            if (raw == null) return EMPTY;
            try (DataInputStream input = new DataInputStream(new BufferedInputStream(raw))) {
                int magic = input.readInt();
                if (magic != MAGIC_IFM1 && magic != MAGIC_IFM2) return EMPTY;
                int vertexCount = input.readInt();
                if (vertexCount <= 0 || vertexCount > MAX_VERTICES) return EMPTY;

                float[] vertices = new float[vertexCount * 3];
                for (int i = 0; i < vertices.length; i++) vertices[i] = input.readFloat();

                float[] colors = null;
                if (magic == MAGIC_IFM2) {
                    colors = new float[vertexCount * 3];
                    for (int i = 0; i < colors.length; i++) colors[i] = input.readFloat();
                }

                int indexCount = input.readInt();
                if (indexCount < 3 || indexCount > MAX_INDICES) return EMPTY;
                indexCount -= indexCount % 3;
                int[] indices = new int[indexCount];
                for (int i = 0; i < indexCount; i++) {
                    int index = input.readInt();
                    if (index < 0 || index >= vertexCount) return EMPTY;
                    indices[i] = index;
                }
                return new CosmeticMesh(vertices, colors, indices);
            }
        } catch (Exception ignored) {
            return EMPTY;
        }
    }

    public boolean isEmpty() {
        return indices.length == 0;
    }

    public void append(MatrixStack matrices, BufferBuilder buffer, int color, float alpha) {
        if (isEmpty()) return;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int baseRed = color >> 16 & 0xFF;
        int baseGreen = color >> 8 & 0xFF;
        int baseBlue = color & 0xFF;
        int outAlpha = Math.max(0, Math.min(255, Math.round(alpha * 255f)));

        for (int i = 0; i < indices.length; i += 3) {
            int a = indices[i] * 3;
            int b = indices[i + 1] * 3;
            int c = indices[i + 2] * 3;

            float abX = vertices[b] - vertices[a];
            float abY = vertices[b + 1] - vertices[a + 1];
            float abZ = vertices[b + 2] - vertices[a + 2];
            float acX = vertices[c] - vertices[a];
            float acY = vertices[c + 1] - vertices[a + 1];
            float acZ = vertices[c + 2] - vertices[a + 2];
            float normalY = abZ * acX - abX * acZ;
            float normalZ = abX * acY - abY * acX;
            float normalLength = (float) Math.sqrt(
                    Math.max(0.000001f,
                            (abY * acZ - abZ * acY) * (abY * acZ - abZ * acY)
                                    + normalY * normalY + normalZ * normalZ));
            float light = 0.52f + 0.34f * Math.abs(normalY / normalLength)
                    + 0.14f * Math.abs(normalZ / normalLength);
            light = Math.min(1f, light);

            vertex(buffer, matrix, a, shaded(baseRed, baseGreen, baseBlue, light, a, outAlpha));
            vertex(buffer, matrix, b, shaded(baseRed, baseGreen, baseBlue, light, b, outAlpha));
            vertex(buffer, matrix, c, shaded(baseRed, baseGreen, baseBlue, light, c, outAlpha));
        }
    }

    private int shaded(int baseRed, int baseGreen, int baseBlue, float light, int offset, int alpha) {
        float r = colors == null ? 1f : colors[offset];
        float g = colors == null ? 1f : colors[offset + 1];
        float b = colors == null ? 1f : colors[offset + 2];
        int red = Math.min(255, Math.round(baseRed * light * r));
        int green = Math.min(255, Math.round(baseGreen * light * g));
        int blue = Math.min(255, Math.round(baseBlue * light * b));
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, int offset, int color) {
        buffer.vertex(matrix, vertices[offset], vertices[offset + 1], vertices[offset + 2]).color(color);
    }

    public static BufferBuilder begin() {
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    public static void draw(BufferBuilder buffer) {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
