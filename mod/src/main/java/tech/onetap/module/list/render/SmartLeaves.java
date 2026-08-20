package tech.onetap.module.list.render;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInformation(moduleName = "SmartLeaves", moduleDesc = "Умные листья", moduleCategory = ModuleCategory.RENDER)
public class SmartLeaves extends Module {

    private static SmartLeaves instance;
    private static final Map<BakedModel, BakedModel> cache = new ConcurrentHashMap<>();

    public SmartLeaves() {
        instance = this;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    /**
     * Возвращает двустороннюю модель для листьев: каждый квад дублируется
     * с обратным порядком вершин, благодаря чему листья видны с обеих сторон.
     */
    public static BakedModel getModel(BlockState state, BakedModel original) {
        if (!isActive() || original == null) return original;
        if (!(state.getBlock() instanceof LeavesBlock)) return original;
        if (original == MinecraftClient.getInstance().getBakedModelManager().getMissingBlockModel()) return original;
        return cache.computeIfAbsent(original, DoubleSidedModel::new);
    }

    public static BakedQuad flipQuad(BakedQuad quad) {
        int[] data = quad.getVertexData();
        int stride = data.length / 4;
        int[] flipped = new int[data.length];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(data, (3 - i) * stride, flipped, i * stride, stride);
        }
        return new BakedQuad(flipped, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.hasShade(), quad.getLightEmission());
    }

    public static class DoubleSidedModel implements BakedModel {
        private final BakedModel delegate;
        private final List<BakedQuad>[] faceQuads;

        @SuppressWarnings("unchecked")
        public DoubleSidedModel(BakedModel delegate) {
            this.delegate = delegate;
            this.faceQuads = new List[Direction.values().length];
            Random random = Random.create();
            for (Direction dir : Direction.values()) {
                List<BakedQuad> original = delegate.getQuads(null, dir, random);
                List<BakedQuad> doubled = new ArrayList<>(original.size() * 2);
                for (BakedQuad quad : original) {
                    doubled.add(quad);
                    doubled.add(flipQuad(quad));
                }
                faceQuads[dir.ordinal()] = List.copyOf(doubled);
            }
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
            if (face != null) return faceQuads[face.ordinal()];
            return delegate.getQuads(state, null, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return delegate.useAmbientOcclusion();
        }

        @Override
        public boolean hasDepth() {
            return delegate.hasDepth();
        }

        @Override
        public boolean isSideLit() {
            return delegate.isSideLit();
        }

        @Override
        public Sprite getParticleSprite() {
            return delegate.getParticleSprite();
        }

        @Override
        public ModelTransformation getTransformation() {
            return delegate.getTransformation();
        }
    }
}
