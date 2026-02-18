package tf.storage.platform.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TFStorageConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    private TFStorageConfig() {
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue tfBagOpenRequiresSneak;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.push("client");
            tfBagOpenRequiresSneak = builder
                .comment("Invert the sneak behavior when opening TF Bag instead of the vanilla inventory")
                .define("tfBagOpenRequiresSneak", false);
            builder.pop();
        }
    }
}
