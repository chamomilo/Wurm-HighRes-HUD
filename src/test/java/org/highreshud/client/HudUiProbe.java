package org.highreshud.client;

import javassist.*;

/** Run native child input/rendering with only font/GPU initialization substituted. */
public final class HudUiProbe {
    public static void main(String[] args) throws Throwable {
        ClassPool pool = new ClassPool(true);
        String gui = "com.wurmonline.client.renderer.gui.";
        String creature = "com.wurmonline.client.renderer.cell.CreatureCellRenderable";
        for (String name : new String[]{gui + "HeadsUpDisplay", creature, "com.wurmonline.client.game.World"}) {
            for (CtClass type = pool.get(name); type != null; type = type.getSuperclass())
                if (type.getClassInitializer() != null) type.getClassInitializer().setBody("{}");
        }
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getWorld")
                .setBody("{ return " + gui + "HudButtonProbe.world; }");
        pool.get(gui + "HeadsUpDisplay").getDeclaredMethod("getActionString")
                .setBody("{ return " + gui + "HudButtonProbe.actionString; }");
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
        pool.get(gui + "HighResFocusBar").getDeclaredMethod("binding")
                .setBody("{ return Integer.toString($1+1); }");
        pool.get("com.wurmonline.client.options.Options").getClassInitializer().setBody("{}");
        CtClass fonts = pool.get(gui + "text.TextFont");
        fonts.getClassInitializer().setBody("{}");
        fonts.getDeclaredMethod("getText", new CtClass[]{pool.get("java.lang.String")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont(12,false); }");
        pool.get(gui + "text.ChamomiloUiV1Fonts").getDeclaredMethod("caption",
                new CtClass[]{CtClass.intType,CtClass.booleanType,pool.get("org.chamomilo.wurm.ui.v1.UiDensity")})
                .setBody("{ return new " + gui + "HudButtonProbe.ProbeFont($1,$2,$3); }");
        CtClass canvas = pool.get(gui + "ChamomiloUiV1Canvas");
        canvas.getDeclaredMethod("fill").setBody("{ " + gui + "HudButtonProbe.canvas.fill($1,$2,$3,$4,$5,$6); }");
        canvas.getDeclaredMethod("texture").setBody("{ return " + gui + "HudButtonProbe.canvas.texture($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11); }");
        new Loader(pool).run(gui + "HudButtonProbe", args);
    }
}
