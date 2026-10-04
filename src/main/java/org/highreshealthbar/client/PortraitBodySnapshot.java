package org.highreshealthbar.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds an attachment-isolated view of the player's paper doll.
 *
 * <p>Wurm's {@code PlayerBodyRenderable.renderPaperDoll} reuses the live
 * {@code AttachedCellRenderable} instances and changes their parent model,
 * matrix and attachment null.  The stock paper-doll job can therefore race
 * the world renderer when the local player is visible.  A portrait must never
 * submit those live attachment objects to its off-screen queue.</p>
 */
final class PortraitBodySnapshot {
    private static final int FOLLOW_UP_REFRESHES = 2;

    private static final Field BODY_PLAYER = field(PlayerBodyRenderable.class, "player");
    private static final Field BODY_PLAYER_MODEL = field(PlayerBodyRenderable.class, "playerModel");
    private static final Field BODY_PAPER_DOLL = field(PlayerBodyRenderable.class, "paperDoll");
    private static final Field BODY_EQUIPMENT = field(PlayerBodyRenderable.class, "equipment");
    private static final Field BODY_EQUIPPED_TOOL = field(PlayerBodyRenderable.class, "equippedTool");
    private static final Field BODY_FACE = field(PlayerBodyRenderable.class, "playerFace");
    private static final Field BODY_KINGDOM = field(PlayerBodyRenderable.class, "kingdomId");
    private static final Field BODY_BLOOD_KINGDOM = field(PlayerBodyRenderable.class, "bloodKingdom");
    private static final Field BODY_MALE = field(PlayerBodyRenderable.class, "isMale");
    private static final Field BODY_SHOW_HAIR_AND_HELMET =
            field(PlayerBodyRenderable.class, "showhairAndHemlet");
    private static final Field BODY_REMOVE_HEAD = field(PlayerBodyRenderable.class, "removeHead");
    private static final Field BODY_SHOW_MASK = field(PlayerBodyRenderable.class, "showMask");
    private static final Field BODY_ONLY_FACE =
            field(PlayerBodyRenderable.class, "isPaperDollOnlyFace");

    private static final Field PAPER_DOLL_MODEL =
            field(BODY_PAPER_DOLL.getType(), "paperDollModel");

    private static final Field ATTACHMENT_COLOR =
            field(AttachedCellRenderable.class, "color");
    private static final Field ATTACHMENT_SECONDARY_COLOR =
            field(AttachedCellRenderable.class, "secondaryColor");
    private static final Field ATTACHMENT_RARITY =
            field(AttachedCellRenderable.class, "rarity");
    private static final Field ATTACHMENT_MALE =
            field(AttachedCellRenderable.class, "isMale");

    private final Logger log;
    private final AtomicBoolean failureReported = new AtomicBoolean();

    private SourceState lastSourceState;
    private PlayerBodyRenderable current;
    private int followUpRefreshes;

    PortraitBodySnapshot(Logger log) {
        this.log = log;
    }

    synchronized PlayerBodyRenderable snapshot(PlayerBodyRenderable source) {
        if (source == null) return null;
        try {
            SourceState sourceState = SourceState.capture(source);
            if (!sourceState.isReady()) return current;

            boolean changed = lastSourceState == null
                    || !lastSourceState.sameAppearance(sourceState);
            if (changed) followUpRefreshes = FOLLOW_UP_REFRESHES;

            if (current == null || changed || followUpRefreshes > 0) {
                current = copy(sourceState);
                if (followUpRefreshes > 0) followUpRefreshes--;
            }
            lastSourceState = sourceState;
            return current;
        } catch (Throwable error) {
            if (failureReported.compareAndSet(false, true)) {
                log.log(Level.WARNING,
                        "Unable to isolate the healthbar portrait from the live player model",
                        error);
            }
            return current;
        }
    }

    private static PlayerBodyRenderable copy(SourceState source) throws Exception {
        // PlayerBodyRenderable already owns a dedicated paper-doll model, separate
        // from the model rendered in the world. Keep that prepared model: cloning
        // ModelResourceWrapper drops parts of its runtime-baked skin/armour state
        // and produces a white, badly framed portrait. The mutable objects that
        // race the world renderer are the equipment attachments, copied below.
        ModelResourceWrapper paperDollModel = source.paperDollModel;
        PlayerBodyRenderable result = new PlayerBodyRenderable(source.player);

        // renderPaperDoll only reads playerModel to reject non-bakeable bodies.
        BODY_PLAYER_MODEL.set(result, source.playerModel);
        Object resultPaperDoll = BODY_PAPER_DOLL.get(result);
        PAPER_DOLL_MODEL.set(resultPaperDoll, paperDollModel);

        AttachedCellRenderable[] equipment =
                new AttachedCellRenderable[source.equipment.length];
        for (int slot = 0; slot < equipment.length; slot++) {
            equipment[slot] = copyAttachment(
                    source.equipment[slot], paperDollModel, false);
        }
        BODY_EQUIPMENT.set(result, equipment);
        BODY_EQUIPPED_TOOL.set(result,
                copyAttachment(source.equippedTool, paperDollModel, true));

        BODY_FACE.set(result, source.face);
        BODY_KINGDOM.setByte(result, source.kingdom);
        BODY_BLOOD_KINGDOM.setByte(result, source.bloodKingdom);
        BODY_MALE.setBoolean(result, source.male);
        BODY_SHOW_HAIR_AND_HELMET.setBoolean(result, source.showHairAndHelmet);
        BODY_REMOVE_HEAD.setBoolean(result, source.removeHead);
        BODY_SHOW_MASK.setBoolean(result, source.showMask);
        BODY_ONLY_FACE.setBoolean(result, source.onlyFace);
        return result;
    }

    private static AttachedCellRenderable copyAttachment(
            AttachedCellRenderable source, ModelResourceWrapper parent,
            boolean heldTool) throws Exception {
        if (source == null) return null;
        String itemName = source.getItemName();
        if (itemName == null || itemName.isEmpty()) return null;

        boolean male = ATTACHMENT_MALE.getBoolean(source);
        String modelName = heldTool
                ? itemName
                : itemName + ".equipped." + (male ? "male" : "female");
        Color primary = color(ATTACHMENT_COLOR.get(source));
        Color secondary = color(ATTACHMENT_SECONDARY_COLOR.get(source));
        return new AttachedCellRenderable(
                modelName,
                parent,
                ATTACHMENT_RARITY.getInt(source),
                male,
                itemName,
                primary.r, primary.g, primary.b,
                secondary.r, secondary.g, secondary.b);
    }

    private static Color color(Object value) {
        return value instanceof Color ? (Color) value : Color.WHITE;
    }

    private static Field field(Class<?> type, String name) {
        try {
            Field result = type.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static final class SourceState {
        private final PlayerObj player;
        private final ModelResourceWrapper playerModel;
        private final ModelResourceWrapper paperDollModel;
        private final AttachedCellRenderable[] equipment;
        private final AttachedCellRenderable equippedTool;
        private final Object face;
        private final String faceKey;
        private final byte kingdom;
        private final byte bloodKingdom;
        private final boolean male;
        private final boolean showHairAndHelmet;
        private final boolean removeHead;
        private final boolean showMask;
        private final boolean onlyFace;

        private SourceState(PlayerObj player,
                            ModelResourceWrapper playerModel,
                            ModelResourceWrapper paperDollModel,
                            AttachedCellRenderable[] equipment,
                            AttachedCellRenderable equippedTool,
                            Object face,
                            String faceKey,
                            byte kingdom,
                            byte bloodKingdom,
                            boolean male,
                            boolean showHairAndHelmet,
                            boolean removeHead,
                            boolean showMask,
                            boolean onlyFace) {
            this.player = player;
            this.playerModel = playerModel;
            this.paperDollModel = paperDollModel;
            this.equipment = equipment;
            this.equippedTool = equippedTool;
            this.face = face;
            this.faceKey = faceKey;
            this.kingdom = kingdom;
            this.bloodKingdom = bloodKingdom;
            this.male = male;
            this.showHairAndHelmet = showHairAndHelmet;
            this.removeHead = removeHead;
            this.showMask = showMask;
            this.onlyFace = onlyFace;
        }

        static SourceState capture(PlayerBodyRenderable source) throws Exception {
            Object paperDoll = BODY_PAPER_DOLL.get(source);
            AttachedCellRenderable[] liveEquipment =
                    (AttachedCellRenderable[]) BODY_EQUIPMENT.get(source);
            return new SourceState(
                    (PlayerObj) BODY_PLAYER.get(source),
                    (ModelResourceWrapper) BODY_PLAYER_MODEL.get(source),
                    (ModelResourceWrapper) PAPER_DOLL_MODEL.get(paperDoll),
                    liveEquipment == null
                            ? new AttachedCellRenderable[0]
                            : liveEquipment.clone(),
                    (AttachedCellRenderable) BODY_EQUIPPED_TOOL.get(source),
                    BODY_FACE.get(source),
                    faceKey(BODY_FACE.get(source)),
                    BODY_KINGDOM.getByte(source),
                    BODY_BLOOD_KINGDOM.getByte(source),
                    BODY_MALE.getBoolean(source),
                    BODY_SHOW_HAIR_AND_HELMET.getBoolean(source),
                    BODY_REMOVE_HEAD.getBoolean(source),
                    BODY_SHOW_MASK.getBoolean(source),
                    BODY_ONLY_FACE.getBoolean(source));
        }

        boolean isReady() {
            return player != null
                    && playerModel != null
                    && playerModel.isLoaded()
                    && paperDollModel != null
                    && paperDollModel.isLoaded();
        }

        boolean sameAppearance(SourceState other) {
            if (player != other.player
                    || playerModel != other.playerModel
                    || paperDollModel != other.paperDollModel
                    || equippedTool != other.equippedTool
                    || face != other.face
                    || !faceKey.equals(other.faceKey)
                    || kingdom != other.kingdom
                    || bloodKingdom != other.bloodKingdom
                    || male != other.male
                    || showHairAndHelmet != other.showHairAndHelmet
                    || removeHead != other.removeHead
                    || showMask != other.showMask
                    || onlyFace != other.onlyFace
                    || equipment.length != other.equipment.length) {
                return false;
            }
            for (int slot = 0; slot < equipment.length; slot++) {
                if (equipment[slot] != other.equipment[slot]) return false;
            }
            return true;
        }

        private static String faceKey(Object value) {
            if (!(value instanceof com.wurmonline.client.renderer.cell.PlayerFace)) {
                return "";
            }
            com.wurmonline.client.renderer.cell.PlayerFace face =
                    (com.wurmonline.client.renderer.cell.PlayerFace) value;
            return face.getHeadType() + ":" + face.getEyeType() + ":"
                    + face.getComplexionType() + ":" + face.getMouthType() + ":"
                    + face.getHairType() + ":" + face.getNoseType() + ":"
                    + face.getFacialHairOrEyeBrow() + ":" + face.getEyeColor() + ":"
                    + face.getHairColorValue() + ":" + face.getSkinColor();
        }
    }
}
