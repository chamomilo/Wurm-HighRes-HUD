package org.highreshud.client;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.CannotCompileException;
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
            String message = "Unable to install unified High-res HUD hooks: " + error;
            LOG.log(Level.SEVERE, message, error);
            throw new RuntimeException(message, error);
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
        installWindowHooks(pool);
    }

    static void installWindowHooks(ClassPool pool) throws Exception {
        String gui = "com.wurmonline.client.renderer.gui.";
        String runtime = "org.highreshud.client.HighResHudRuntime.";
        installCharacterHooks(pool);
        CtClass hud = pool.get(gui + "HeadsUpDisplay");
        hud.getDeclaredMethod("beginRender").insertBefore(runtime+"inventoryDragFrame(this,$3,$4);");
        hud.getDeclaredMethod("mouseDragged").insertAfter(runtime+"inventoryDragFrame(this,$1,$2);");
        hud.getDeclaredMethod("mouseReleased").insertBefore("{if("+runtime+"inventoryDrop(this,$1,$2,$3))return;}");
        hud.getDeclaredMethod("resetDrag").insertAfter(runtime+"inventoryDragCleared();");
        hud.getDeclaredMethod("addInventoryWindow").insertAfter(runtime + "containerOpened(this,$1);");
        hud.getDeclaredMethod("removeInventoryWindow").insertBefore(runtime + "containerClosed(this,$1);");
        for (String method : new String[]{"addInventoryContainerWindow", "toggleInventoryContainerWindow"})
            hud.getDeclaredMethod(method).insertAfter(runtime + "inventoryContainerOpened(this,$1);");
        hud.getDeclaredMethod("removeDynamicComponent").insertBefore(runtime + "windowRemoved(this,$1);");
        hud.getDeclaredMethod("removeComponent").insertBefore(runtime + "windowHidden(this,$1);");
        hud.getDeclaredMethod("reconnected").insertAfter(runtime + "windowsReconnected(this);");
        hud.getDeclaredMethod("addComponent").insertAfter("{if($_)" + runtime + "windowShown(this,$1);}");
        // Avatar2/Keybinder define GUI base classes before preInit. Instrument the
        // still-unloaded HUD/renderer callers, never defrost already defined classes.
        pool.get(gui + "Renderer").getDeclaredMethod("execute").instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("render"))
                    call.replace("{if(!" + runtime + "renderWindow($0,$1))$proceed($$);}");
            }
        });
        // Press/hover call the window directly; hooking HUD.getComponentAt alone
        // leaves its resize corner painted but unreachable during a real press.
        for (String method : new String[]{"getComponentAt", "mousePressed", "mouseMoved"}) hud.getDeclaredMethod(method).instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("getComponentAt"))
                    call.replace("{$_=" + runtime + "windowComponentAt($0,$1,$2);if($_==null)$_=$proceed($$);}");
            }
        });
        hud.getDeclaredMethod("mouseReleased").instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("leftReleased"))
                    call.replace("{" + runtime + "beforeWindowButtonRelease($0,$1,$2);$proceed($$);}");
            }
        });
        // Resolve hints at pick time, including a language change before the next paint.
        // Leave preloaded WButton/GUI base classes intact.
        hud.getDeclaredMethod("pick").instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("pick"))
                    call.replace("{if(!" + runtime + "pickComponentHint($0,$1,$2,$3))$proceed($$);}");
            }
        });
        hud.getDeclaredMethod("mouseWheeled").instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("mouseWheeled"))
                    call.replace("{if(!" + runtime + "windowScroll($0,$1,$2,$3))$proceed($$);}");
            }
        });
        hud.getDeclaredMethod("mouseDragged").instrument(new ExprEditor() {
            @Override public void edit(MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals(gui + "WurmComponent") && call.getMethodName().equals("mouseDragged"))
                    call.replace("{$proceed($$);" + runtime + "windowResized($0);}");
            }
        });
        pool.get(gui + "ChatPanelComponent").getDeclaredMethod("isMinimized")
                .insertBefore("{if(" + runtime + "hiddenChatManager(this))return true;}");
        pool.get(gui + "ChatPanelComponent").getDeclaredMethod("addText", new CtClass[]{pool.get("java.lang.String"), pool.get("java.lang.String"),
                CtClass.floatType, CtClass.floatType, CtClass.floatType, CtClass.booleanType})
                .insertBefore("{if(" + runtime + "hiddenSkillTick($1,$2))return;}");
        pool.get(gui + "ChatPanelComponent").getDeclaredMethod("addText", new CtClass[]{pool.get("java.lang.String"), pool.get("java.util.List"), CtClass.booleanType})
                .insertBefore("{if(" + runtime + "hiddenSkillTickSegments($1,$2))return;}");
    }

    private static void installCharacterHooks(ClassPool pool) throws Exception {
        CtClass character=pool.get("com.wurmonline.client.renderer.gui.PaperDollInventory");
        String bridge="com.wurmonline.client.renderer.gui.HighResWindowIntegration.";
        for(String method:new String[]{"getPositionX","getPositionY","getWoundPositionX","getWoundPositionY"})
            character.getDeclaredMethod(method).insertBefore("{if("+bridge+"characterOwned(this))return "+bridge+"characterPosition(this,$1,"+method.endsWith("Y")+","+method.startsWith("getWound")+");}");
        for(String method:new String[]{"getSlotNumberAt","getWoundButtonAt"})
            character.getDeclaredMethod(method).insertBefore("{if("+bridge+"characterOwned(this))return "+bridge+"characterHit(this,$1,$2,"+method.startsWith("getWound")+");}");
        character.getDeclaredMethod("checkIfClickedAtPlayer").insertBefore("{if("+bridge+"characterOwned(this))return "+bridge+"characterPlayerHit(this,$1,$2);}");
        character.getDeclaredMethod("getWoundAt").insertBefore("{if("+bridge+"characterOwned(this))return "+bridge+"characterWoundAt(this,$1,$2);}");
        character.getDeclaredMethod("leftPressed").insertAfter(bridge+"characterPressed(this,$1,$2);");
        character.getDeclaredMethod("leftReleased").insertAfter(bridge+"characterReleased(this,$1,$2);");
        for(String method:new String[]{"pick","rightPressed","leftPressed","getDraggableComponentAt","getInventoryMetaItemAt","itemDropped"})
            character.getDeclaredMethod(method).insertBefore(bridge+"characterLayout(this);");
        character.getDeclaredMethod("pick").insertBefore("{if("+bridge+"characterPick(this,$1,$2,$3))return;}");
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
        fightWindow.getMethod("setOptions", "([B)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.focusOptions(this);");
        fightWindow.getMethod("updatePosition", "(FFB)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.focusPosition(this,$3);");
        fightWindow.getMethod("setFocusLevel", "(BLjava/lang/String;)V").insertAfter(
                "org.highreshud.client.HighResHudRuntime.focusLevel(this,$1,$2);");

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
