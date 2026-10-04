package org.highreshud.client;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Installs every shared Wurm hook once and routes it through one handler. */
public final class UnifiedHookInstaller {
    private static final Logger LOG = Logger.getLogger("HighResHud.Hooks");
    private static boolean installed;

    private UnifiedHookInstaller() {
    }

    public static synchronized void install() {
        if (installed) return;
        try {
            ClassPool pool = HookManager.getInstance().getClassPool();
            installOn(pool);
            installed = true;
            LOG.info("Unified High-res HUD hooks installed");
        } catch (Throwable error) {
            LOG.log(Level.SEVERE,
                    "Unable to install unified High-res HUD hooks", error);
            throw new RuntimeException(
                    "Unable to install unified High-res HUD hooks", error);
        }
    }

    static void installOn(ClassPool pool) throws Exception {
        makeResourceLookupConcurrentSafe(pool);
        installResourceHook(pool);
        installHudHooks(pool);
        installSelectHooks(pool);
        installWorldOutlineHook(pool);
        installActionHooks(pool);
        installCombatHooks(pool);
        installHealthbarHooks(pool);
    }

    private static void installResourceHook(ClassPool pool) throws Exception {
        CtClass icons = pool.getCtClass(
                "com.wurmonline.client.resources.textures.IconLoader");
        icons.getDeclaredMethod("initIcons").insertBefore(
                "org.highreshud.client.HighResHudRuntime.resourcesReady();");
    }

    private static void installHudHooks(ClassPool pool) throws Exception {
        CtClass hud = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.HeadsUpDisplay");
        hud.getDeclaredMethod("addComponent").insertBefore(
                "{if(org.highreshud.client.HighResHudRuntime"
                        + ".redirectFightWindow(this,$1))return false;}");
        hud.getMethod("init", "(II)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.hudReady(this);");
        hud.getMethod("beginRender", "(FZII)V").insertBefore(
                "org.highreshud.client.HighResHudRuntime.beginFrame(this);");
        hud.getMethod("endRender", "()V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.endFrame(this);");
        hud.getMethod("mouseMoved", "(II)V").insertBefore(
                "org.highreshud.client.HighResHudRuntime.mouseMoved(this,$1,$2);");
        hud.getMethod("mouseWheeled", "(III)V").insertBefore(
                "{if(org.highreshud.client.HighResHudRuntime"
                        + ".mouseWheeled(this,$1,$2,$3))return;}");
        hud.getMethod("setAction", "(Ljava/lang/String;F)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.actionStarted($1,$2);");
        hud.getMethod("setTargetCreature",
                        "(JLcom/wurmonline/client/renderer/cell/"
                                + "CreatureCellRenderable;)V")
                .insertAfter("org.highreshud.client.HighResHudRuntime"
                        + ".targetChanged(this);");
        hud.getMethod("textMessage",
                        "(Ljava/lang/String;FFFLjava/lang/String;)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".serverText($1,$5))return;}");
        hud.getMethod("textMessage",
                        "(Ljava/lang/String;Ljava/util/List;)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".serverMulticolorText($1,$2))return;}");
        hud.getMethod("popupReceived",
                        "(BLjava/util/List;Ljava/lang/String;)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".popupReceived(this,$1,$2))return;}");
        hud.getMethod("addInventoryWindow",
                        "(Lcom/wurmonline/client/game/inventory/"
                                + "InventoryMetaWindowView;)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".inventoryWindow(this,$1))return;}");
    }

    private static void installSelectHooks(ClassPool pool) throws Exception {
        CtClass select = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.SelectBar");
        select.getMethod("setProgress", "(FLjava/lang/String;Z)V")
                .insertAfter("org.highreshud.client.HighResHudRuntime"
                        + ".selectProgress(this,$1,$2,$3);");
        select.getMethod("addActions", "(BLjava/util/List;)V")
                .insertBefore("org.highreshud.client.HighResHudRuntime"
                        + ".selectActions(this,$1,$2);");
        CtMethod setSelected = select.getDeclaredMethod("setSelected");
        setSelected.insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                + ".suppressSelection(this))$1=null;}");
        setSelected.insertAfter("org.highreshud.client.HighResHudRuntime"
                + ".selectionChanged(this);");
    }

    private static void installWorldOutlineHook(ClassPool pool)
            throws Exception {
        CtClass worldRender = pool.getCtClass(
                "com.wurmonline.client.renderer.WorldRender");
        CtMethod original = worldRender.getDeclaredMethod("renderPickedItem");
        original.setName("highresHudRenderHoveredItem");
        worldRender.addMethod(CtNewMethod.make(
                "private void renderPickedItem("
                        + "com.wurmonline.client.renderer.backend.Queue queue){"
                        + "this.highresHudRenderHoveredItem(queue);"
                        + "if(this.itemPlacer.isActive())return;"
                        + "com.wurmonline.client.renderer.PickableUnit old="
                        + "this.pickedItem;"
                        + "com.wurmonline.client.renderer.PickableUnit selected="
                        + "org.highreshud.client.HighResHudRuntime"
                        + ".selectedWorldOutline();"
                        + "if(selected==null)return;"
                        + "try{this.pickedItem=selected;"
                        + "this.highresHudRenderHoveredItem(queue);}"
                        + "finally{this.pickedItem=old;}"
                        + "}", worldRender));
    }

    private static void installActionHooks(ClassPool pool) throws Exception {
        CtClass connection = pool.getCtClass(
                "com.wurmonline.client.comm.SimpleServerConnectionClass");
        connection.getMethod("sendAction",
                        "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V")
                .insertBefore("org.highreshud.client.HighResHudRuntime"
                        + ".actionSent($1,$2,$3);");
        connection.getMethod("sendSingleAction",
                        "(JJLcom/wurmonline/shared/constants/PlayerAction;)V")
                .insertBefore("org.highreshud.client.HighResHudRuntime"
                        + ".singleActionSent($1,$2,$3);");
    }

    private static void installCombatHooks(ClassPool pool) throws Exception {
        CtClass fightWindow = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.FightWindowComponent");
        fightWindow.getMethod("toggleFighting", "(Z)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.combatChanged($1);");

        CtClass listener = pool.getCtClass(
                "com.wurmonline.client.comm.ServerConnectionListenerClass");
        listener.getDeclaredMethod("playDeadThenReplaceWithCorpse")
                .insertBefore("org.highreshud.client.HighResHudRuntime"
                        + ".corpseCreated($1,$2);");

        CtClass cellRenderer = pool.getCtClass(
                "com.wurmonline.client.renderer.cell.CellRenderer");
        cellRenderer.getMethod("addRenderable",
                        "(Lcom/wurmonline/client/renderer/cell/CellRenderable;)V")
                .insertAfter("org.highreshud.client.HighResHudRuntime"
                        + ".renderableAdded($1);");

        CtClass creature = pool.getCtClass(
                "com.wurmonline.client.renderer.cell.CreatureCellRenderable");
        creature.getMethod("setAttitude", "(I)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime"
                        + ".attitudeChanged(this,$1);");
    }

    private static void installHealthbarHooks(ClassPool pool)
            throws Exception {
        CtClass listener = pool.getCtClass(
                "com.wurmonline.client.comm.ServerConnectionListenerClass");
        listener.getMethod("textMessage",
                        "(Ljava/lang/String;FFFLjava/lang/String;B)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".listenerText($5))return;}");
        listener.getMethod("showBMLForm",
                        "(SLjava/lang/String;IIFFZZFFFLjava/lang/String;)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".titleBml($12))return;}");

        CtClass chat = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.ChatPanelComponent");
        chat.getMethod("addText",
                        "(Ljava/lang/String;Ljava/lang/String;FFFZ)V")
                .insertBefore("{if(org.highreshud.client.HighResHudRuntime"
                        + ".vehicleEvent($1,$2))return;}");
    }

    /** Keeps serverpacks reflection safe under concurrent texture requests. */
    private static void makeResourceLookupConcurrentSafe(ClassPool pool)
            throws Exception {
        CtClass resources = pool.getCtClass(
                "com.wurmonline.client.resources.Resources");
        CtMethod method = resources.getDeclaredMethod("findResource",
                new CtClass[]{pool.getCtClass("java.lang.String")});
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            method.setModifiers((modifiers
                    & ~(Modifier.PRIVATE | Modifier.PROTECTED))
                    | Modifier.PUBLIC);
        }
    }
}
