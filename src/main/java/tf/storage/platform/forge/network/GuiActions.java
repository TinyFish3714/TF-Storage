package tf.storage.platform.forge.network;

public final class GuiActions {

    public static final class GuiType {
        public static final int CHEST = 0;
        public static final int BAG = 1;

        private GuiType() {
        }
    }

    public static final class Bag {
        public static final int SELECT_MODULE = 0;
        public static final int MOVE_ITEMS = 1;
        public static final int SORT_ITEMS = 2;
        public static final int TOGGLE_REGION_LOCK = 3;
        public static final int TOGGLE_MODES = 5;
        public static final int TOGGLE_SHIFTCLICK = 6;
        public static final int TOGGLE_SHIFTCLICK_DOUBLETAP = 7;

        private Bag() {
        }
    }

    public static final class Chest {
        public static final int SELECT_MEMORY_CARD = 0;
        public static final int MOVE_ITEMS = 2;
        public static final int SORT_ITEMS = 3;

        private Chest() {
        }
    }

    private GuiActions() {
    }
}
