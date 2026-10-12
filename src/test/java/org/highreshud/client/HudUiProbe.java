package org.highreshud.client;

import javassist.*;

/** Run native child input/rendering with only font/GPU initialization substituted. */
public final class HudUiProbe {
    public static void main(String[] args) throws Throwable {
        ClassPool pool = new ClassPool(true);
        String olderSdk = System.getProperty("olderSharedUiJar");
        if (olderSdk != null) pool.insertClassPath(olderSdk);
        String gui = "com.wurmonline.client.renderer.gui.";
        String creature = "com.wurmonline.client.renderer.cell.CreatureCellRenderable";
        for (String name : new String[]{gui + "HeadsUpDisplay", creature, "com.wurmonline.client.game.World", "com.wurmonline.client.game.PlayerObj"}) {
            for (CtClass type = pool.get(name); type != null; type = type.getSuperclass())
                if (type.getClassInitializer() != null) type.getClassInitializer().setBody("{}");
        }
        pool.get(gui + "HeadsUpDisplay").getClassInitializer().setBody("{scissor=new com.wurmonline.client.renderer.backend.ScissorControl();}");
        pool.get("com.wurmonline.client.renderer.backend.ScissorControl").getDeclaredMethod("pushClip")
                .setBody("{return " + gui + "HudButtonProbe.clip($1,$2,$3,$4);}");
        pool.get("com.wurmonline.client.renderer.backend.ScissorControl").getDeclaredMethod("popClip")
                .setBody("{" + gui + "HudButtonProbe.unclip();}");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getWorld")
                .setBody("{ return " + gui + "HudButtonProbe.world; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getActionString")
                .setBody("{ return " + gui + "HudButtonProbe.actionString; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("isShiftDown").setBody("{return false;}");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("isControlDown").setBody("{return false;}");
        pool.get("com.wurmonline.client.game.PlayerObj").getDeclaredMethod("isSleepBonusActive").setBody("{ return false; }");
        pool.get(gui + "HealthBar").getDeclaredMethod("showCCFP").setBody("{ return true; }");
        // Use temporary user preferences in this isolated fixture, never the real profile.
        pool.get("org.chamomilo.wurm.update.UpdatePreferences").getDeclaredMethod("shared")
                .setBody("{ return " + gui + "HudButtonProbe.updaterPreferences; }");
        pool.get("com.wurmonline.client.game.World").getDeclaredMethod("getPlayerPosX").setBody("{ return 0f; }");
        pool.get("com.wurmonline.client.game.World").getDeclaredMethod("getPlayerPosY").setBody("{ return 0f; }");
        pool.get(creature).getMethod("getXPos","()F").setBody("{ return 1234.9f; }");
        pool.get(creature).getMethod("getYPos","()F").setBody("{ return 0f; }");
        pool.get(creature).getDeclaredMethod("getCurrentFightStance").setBody("{ return 0; }");
        pool.get(gui + "WurmComponent").getClassInitializer().setBody("{}");
        pool.get(gui + "WurmComponent").getDeclaredMethod("fillRect")
                .setBody("{ " + gui + "HudButtonProbe.canvas.fill(new org.chamomilo.wurm.ui.v1.UiColor($2,$3,$4),$5,$6,$7,$8,$9); }");
        pool.get(gui + "WurmComponent").getDeclaredMethod("render")
                .setBody("{ this.renderComponent($1,$2); }"); // Substitute only the native GPU scissor stack.
        CtClass renderer = pool.get(gui + "Renderer");
        if (renderer.getClassInitializer() != null) renderer.getClassInitializer().setBody("{}");
        renderer.getDeclaredMethod("texturedQuadAlphaBlend")
                .setBody("{ if($2==null) " + gui + "HudButtonProbe.combatIconQuad($3,$4,$5,$6,$7,$8,$9,$10);"
                        + " else " + gui + "HudButtonProbe.atlasQuad($3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14); }");
        CtClass textures = pool.get("com.wurmonline.client.resources.textures.ResourceTextureLoader");
        if (textures.getClassInitializer() != null) textures.getClassInitializer().setBody("{}");
        textures.getDeclaredMethod("getNowrapLinearTexture").setBody("{ return null; }");
        textures.getDeclaredMethod("getNearestTexture").setBody("{ return null; }");
        pool.get(gui + "HighResFocusBar").getDeclaredMethod("binding")
                .setBody("{ return Integer.toString($1+1); }");
        pool.get("com.wurmonline.client.options.Options").getClassInitializer().setBody("{}");
        CtClass fonts = pool.get(gui + "text.TextFont");
        fonts.getClassInitializer().setBody("{}");
        fonts.getDeclaredMethod("getText", new CtClass[]{pool.get("java.lang.String")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont(12,false); }");
        fonts.getDeclaredMethod("getFixedSizeText").setBody("{return new " + gui + "HudButtonProbe.ProbeFont(14,false,org.chamomilo.wurm.ui.v1.UiDensity.LOW);}");
        pool.get(gui + "text.ChamomiloUiV1Fonts").getDeclaredMethod("caption",
                new CtClass[]{CtClass.intType,CtClass.booleanType,pool.get("org.chamomilo.wurm.ui.v1.UiDensity")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont($1,$2,$3); }");
        CtClass canvas = pool.get(gui + "ChamomiloUiV1Canvas");
        canvas.getDeclaredMethod("fill").setBody("{ " + gui + "HudButtonProbe.canvas.fill($1,$2,$3,$4,$5,$6); }");
        canvas.getDeclaredMethod("texture").setBody("{ return " + gui + "HudButtonProbe.canvas.texture($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11); }");
        pool.get(gui + "WurmTreeList").getClassInitializer().setBody("{}");
        for (String name : new String[]{"InventoryListComponent", "InventoryListComponent$SingleTreeListItem", "InventoryListComponent$GroupTreeListItem",
                "InventoryListComponent$InventoryTreeListItem", "DefaultTab", "WurmTreeList", "WurmTextPanel", "ChatPanelComponent"}) {
            pool.get(gui + name).instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    try {
                        if (call.getClassName().equals("com.wurmonline.client.options.BooleanOption") && call.getMethodName().equals("value"))
                            call.replace("{$_=false;}");
                    } catch (ClassCastException legacyLambda) { }
                }
            });
        }
        // Keep list/layout/editor/model logic; substitute only native GPU body drawing.
        pool.get(gui + "WurmTreeList$TreeListPanel").getDeclaredMethod("renderComponent").setBody("{}");
        pool.get(gui + "WurmTextPanel").getDeclaredMethod("renderComponent").setBody("{}");
        pool.get(gui + "WurmInputField").getDeclaredMethod("renderComponent").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals("com.wurmonline.client.options.MultiOption") && call.getMethodName().equals("value")) call.replace("{$_=0;}");
            }
        });
        pool.get(gui + "HudSettings").getDeclaredMethod("registerComponent").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.FieldAccess field) throws CannotCompileException {
                if (field.isReader() && field.getClassName().equals("com.wurmonline.client.options.Options") && field.getFieldName().equals("guiSkins"))
                    field.replace("{$_=" + gui + "HighResWindowsProbe.guiSkin();}");
            }
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals("com.wurmonline.client.resources.textures.ResourceTextureLoader") && call.getMethodName().equals("getTexture"))
                    call.replace("{$_=null;}");
            }
        });
        if (args.length > 1 && args[0].equals("--windows")) {
            pool.get(gui+"HighResCharacterArtwork").getDeclaredMethod("draw")
                    .setBody("{"+gui+"HudButtonProbe.characterArtwork($$);}");
            pool.get("com.wurmonline.client.game.World").getDeclaredMethod("getServerConnection")
                    .setBody("{return "+gui+"HighResWindowsProbe.connection;}");
            pool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("sendMoveSomeItems")
                    .setBody("{"+gui+"HighResWindowsProbe.movedTarget=$1;"+gui+"HighResWindowsProbe.movedIds=$2;}");
            pool.get("com.wurmonline.client.comm.SimpleServerConnectionClass").getDeclaredMethod("sendAction",new CtClass[]{CtClass.longType,pool.get("long[]"),pool.get("com.wurmonline.shared.constants.PlayerAction")})
                    .setBody("{"+gui+"HighResWindowsProbe.actionCount++;"+gui+"HighResWindowsProbe.actionId=$3.getId();"+gui+"HighResWindowsProbe.actionTargets=$2;}");
            pool.get(gui+"HeadsUpDisplay").getDeclaredMethod("sendAction",new CtClass[]{pool.get("com.wurmonline.shared.constants.PlayerAction"),pool.get("long[]")})
                    .instrument(new javassist.expr.ExprEditor() {
                        @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                            if(call.getMethodName().equals("getMaxNumberOfActions"))call.replace("{$_=4;}");
                        }
                    });
            pool.get(gui+"PaperDollRenderer").getDeclaredMethod("requestRender",new CtClass[]{pool.get("com.wurmonline.client.renderer.PlayerBodyRenderable"),CtClass.floatType,CtClass.intType,CtClass.intType,CtClass.booleanType})
                    .setBody("{"+gui+"HighResWindowsProbe.modelWidth=$3;"+gui+"HighResWindowsProbe.modelHeight=$4;}");
            pool.get(gui+"PaperDollRenderer").getDeclaredMethod("getTexture").setBody("{return null;}");
            // Keep both native container types and their HUD add/toggle/close paths.
            // Only their access to the player's inventory is supplied by the fixture.
            javassist.expr.ExprEditor playerInventory = new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    if (call.getClassName().equals("com.wurmonline.client.game.World") && call.getMethodName().equals("getInventoryManager"))
                        call.replace("{$_=null;}");
                    else if (call.getClassName().equals("com.wurmonline.client.game.inventory.InventoryMetaWindowManager") && call.getMethodName().equals("getPlayerInventory"))
                        call.replace("{$_=" + gui + "HighResWindowsProbe.playerInventory;}");
                }
            };
            for (String method : new String[]{"addInventoryContainerWindow", "toggleInventoryContainerWindow"})
                pool.get(gui + "HeadsUpDisplay").getDeclaredMethod(method).instrument(playerInventory);
            pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("setEnabledOnPlayerStateButton").setBody("{}");
            for (CtConstructor constructor : pool.get(gui + "InventoryContainerWindow").getDeclaredConstructors()) {
                constructor.instrument(playerInventory);
                constructor.instrument(new javassist.expr.ExprEditor() {
                    @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                        if (call.getClassName().equals("com.wurmonline.client.options.BooleanOption") && call.getMethodName().equals("value"))
                            call.replace("{$_=true;}"); // Do not request empty contents from a server in this offline probe.
                    }
                });
            }
            pool.get(gui + "InventoryContainerWindow").getDeclaredMethod("closePressed").instrument(playerInventory);
            // Keep the complete native press/focus/drag/release route; only
            // unrelated world/cursor and active-window notifications are absent.
            pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("mousePressed").instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    String name = call.getMethodName();
                    if (name.equals("getClient") || name.equals("getEventHandler")) call.replace("{$_=null;}");
                    else if (name.equals("getIsFreeLookMode")) call.replace("{$_=false;}");
                }
            });
            for (String method : new String[]{"resetDrag", "hideItemPlate", "setActiveWindow", "resetKeyboardFocus"})
                pool.get(gui + "HeadsUpDisplay").getDeclaredMethod(method).setBody("{}");
            for (CtConstructor constructor : pool.get(gui + "WurmDropDown").getDeclaredConstructors())
                constructor.instrument(new javassist.expr.ExprEditor() {
                    @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                        if (!call.getClassName().equals("com.wurmonline.client.renderer.backend.VertexBuffer")) return;
                        if (call.getMethodName().equals("create")) call.replace("{$_=null;}");
                        else if (call.getMethodName().equals("lock")) call.replace("{$_=java.nio.FloatBuffer.allocate(64);}");
                        else if (call.getMethodName().equals("unlock")) call.replace("{}");
                    }
                });
            pool.get(gui + "HighResSkillDetailDialog").getDeclaredMethod("restoreNativeMessages").instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    if (call.getClassName().equals("com.wurmonline.client.options.MultiOption") && call.getMethodName().equals("set"))
                        call.replace("{" + gui + "HighResWindowsProbe.minimumTicksOption=$1;}");
                }
            });
            // Isolate GPU skins and unrelated HUD menu/keyboard callbacks in the window fixture.
            pool.get(gui + "WurmComponent").getDeclaredMethod("drawTexture").setBody("{}");
            renderer.getDeclaredMethod("texturedQuadAlphaBlend").setBody("{}");
            pool.get(gui + "WurmComponent").getDeclaredMethod("setInitialSize", new CtClass[]{CtClass.intType, CtClass.intType,
                    CtClass.booleanType, CtClass.booleanType, CtClass.floatType, CtClass.floatType}).instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    if (call.getClassName().equals("com.wurmonline.client.options.RangeOption") && call.getMethodName().equals("value")) call.replace("{$_=14;}");
                }
                @Override public void edit(javassist.expr.FieldAccess field) throws CannotCompileException {
                    if (field.isReader() && field.getClassName().equals("com.wurmonline.client.options.RangeOption")
                            && field.getFieldName().equals("defaultValue")) field.replace("{$_=14;}");
                }
            });
            pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("addComponent")
                    .setBody("{if(components.contains($1))return false;components.add($1);$1.setPosition($1.x,$1.y);return true;}");
            pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("removeComponent")
                    .setBody("{return components.remove($1);}");
            for (String method : new String[]{"createOrRestoreWindowState", "saveWindowState"})
                pool.get(gui + "HeadsUpDisplay").getDeclaredMethod(method).instrument(new javassist.expr.ExprEditor() {
                    @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                        if (call.getClassName().equals("com.wurmonline.client.settings.SavePosManager")) {
                            if (call.getMethodName().equals("hasComponentRawPositions")) call.replace("{$_=false;}");
                            else call.replace("{}");
                        }
                    }
                });
            pool.get(gui + "ItemListWindow").getDeclaredMethod("closePressed").instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    String name = call.getMethodName();
                    if (name.equals("getServerConnection")) call.replace("{$_=null;}");
                    else if (name.equals("sendInventoryWindowClosed")) call.replace("{" + gui + "HighResWindowsProbe.closedId=$1;}");
                }
            });
        }
        // Only isolate the freelook query; keep native HUD hit testing and pick dispatch.
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("pick").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if (call.getMethodName().equals("getClient") || call.getMethodName().equals("getEventHandler")) call.replace("{$_=null;}");
                else if (call.getMethodName().equals("getIsFreeLookMode")) call.replace("{$_=false;}");
            }
        });
        // These are offline rendering/input fixtures, without an installed mods
        // directory. The real constructor election is exercised by HudLoaderProbe.
        pool.get("org.chamomilo.wurm.update.SharedUpdaterBootstrap").getDeclaredMethod("initialize").setBody("{}");
        UnifiedHookInstaller.installWindowHooks(pool);
        if (args.length > 1 && args[0].equals("--windows"))
            new Loader(pool).run(gui + "HighResWindowsProbe", new String[]{args[1]});
        else new Loader(pool).run(gui + "HudButtonProbe", args);
    }
}
