package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.science.ScienceEngine;
import java.util.List;

public class MagicScriptEngine {

    private static final int MAX_INSTRUCTIONS = 100;

    public static void execute(SpellContext ctx, List<String> script) throws ScriptExecutionException {
        ctx.script.clear();
        ctx.script.addAll(script);
        ctx.science.visuals.scriptHash = generateScriptHash(script);

        int instructionCount = 0;
        int lineIndex = 0;

        // 制御構造用
        boolean skipBlock = false;
        int loopCount = 0;
        int loopStartLine = -1;

        for (int i = 0; i < script.size(); i++) {
            String line = script.get(i).trim();
            lineIndex = i + 1;

            //System.out.println("DEBUG: Processing: " + line);

            if (line.isEmpty() || line.startsWith("//")) continue;

            // ループ制限
            instructionCount++;
            if (instructionCount > MAX_INSTRUCTIONS) {
                throw new ScriptExecutionException("命令数オーバーフロー (Max " + MAX_INSTRUCTIONS + ")", lineIndex);
            }

            ctx.cost += 1; // 命令コスト

            // --- 構文解析 ---
            if (line.equals("}")) {
                if (skipBlock) {
                    skipBlock = false;
                } else if (loopCount > 0) {
                    loopCount--;
                    if (loopCount > 0) {
                        i = loopStartLine;
                    } else {
                        loopStartLine = -1;
                    }
                }
                continue;
            }

            if (skipBlock) continue;

            // IF文 (簡易実装)
            if (line.startsWith("IF")) {
                // TODO: 条件判定の実装 (Distance < 5 など)
                // 今回は常にtrueとして通過させるか、簡易パーサーを書く
                // boolean condition = evaluate(line, ctx);
                // if (!condition) skipBlock = true;
                continue;
            }

            // REPEAT文
            if (line.startsWith("REPEAT")) {
                if (loopStartLine != -1) throw new ScriptExecutionException("ネストされたループは未対応", lineIndex);
                int count = parseIntegerArg(line);
                if (count > 20) throw new ScriptExecutionException("ループ回数過多", lineIndex);
                loopCount = count;
                loopStartLine = i;
                ctx.cost += (count * 5);
                continue;
            }

            // --- コマンド実行 ---
            try {
                executeCommand(line, ctx);
            } catch (Exception e) {
                throw new ScriptExecutionException(e.getMessage(), lineIndex);
            }
        }

        // 最終的な見た目計算
        ScienceEngine.simulateVisuals(ctx.science);
    }

    public static SpellContext simulate(List<String> script) {
        SpellContext ctx = new SpellContext();
        try {
            execute(ctx, script);
        } catch (Exception e) {
            ctx.cost = 9999;
        }
        return ctx;
    }


    private static void executeCommand(String line, SpellContext ctx) {
        String cmd = line.trim();

        // エイリアス（短縮名）の展開
        if (cmd.startsWith("Attr.Decomp")) cmd = "Attr.Decomposition()";
        if (cmd.startsWith("Attr.Vib")) cmd = "Attr.Vibration()";

        // 1. 作用 (Action)
        if (cmd.startsWith("Action.Shoot"))    MagicComponentType.ACT_SHOOT.apply(ctx);
        else if (cmd.startsWith("Action.Explode"))  MagicComponentType.ACT_EXPLODE.apply(ctx);
        else if (cmd.startsWith("Action.Restore"))  MagicComponentType.ACT_RESTORE.apply(ctx);
        else if (cmd.startsWith("Action.Move"))     MagicComponentType.ACT_MOVE.apply(ctx);   // 追加
        else if (cmd.startsWith("Action.Defend"))   MagicComponentType.ACT_DEFEND.apply(ctx); // 追加

            // 2. 属性 (Attribute)
        else if (cmd.startsWith("Attr.Air"))   MagicComponentType.ATTRIB_AIR.apply(ctx);
        else if (cmd.startsWith("Attr.Vibration")) MagicComponentType.ATTRIB_VIBRATION.apply(ctx);
        else if (cmd.startsWith("Attr.Decomposition")) MagicComponentType.ATTRIB_DECOMPOSITION.apply(ctx);
        else if (cmd.startsWith("Attr.Fire"))  MagicComponentType.ATTRIB_FIRE.apply(ctx);  // 追加
        else if (cmd.startsWith("Attr.Ice"))   MagicComponentType.ATTRIB_ICE.apply(ctx);   // 追加
        else if (cmd.startsWith("Attr.Accel")) MagicComponentType.ATTRIB_ACCEL.apply(ctx); // 追加
        else if (cmd.startsWith("Attr.Weight")) MagicComponentType.ATTRIB_WEIGHT.apply(ctx); // 追加

            // 3. 強化 (Modifier)
        else if (cmd.startsWith("Mod.Power"))  MagicComponentType.MOD_POWER.apply(ctx); // MOD_POWER_UP -> Mod.Power
        else if (cmd.startsWith("Mod.Range"))  MagicComponentType.MOD_RANGE.apply(ctx); // 追加
        else if (cmd.startsWith("Mod.Strategic")) MagicComponentType.MOD_STRATEGIC.apply(ctx);

        if (cmd.startsWith("Mod.Origin")) {
            String arg = parseStringArg(cmd); // カッコの中身を取得
            if (arg.equals("RANDOM_AIR")) ctx.science.visuals.anchorType = EnumMagicAnchor.RANDOM_AIR;
            else if (arg.equals("CASTER")) ctx.science.visuals.anchorType = EnumMagicAnchor.CASTER_ANCHORED;
            else if (arg.equals("TARGET")) ctx.science.visuals.anchorType = EnumMagicAnchor.TARGET_ANCHORED;
            else if (arg.equals("FIXED")) ctx.science.visuals.anchorType = EnumMagicAnchor.WORLD_FIXED;
            return;
        }

            // 4. 実行コマンド
        else if (cmd.startsWith("Cast()")) {
            if (ctx.isSimulation) return;
            SpellExecutor.execute(ctx);
        }
    }

    private static int parseIntegerArg(String line) {
        try {
            int start = line.indexOf('(') + 1;
            int end = line.indexOf(')');
            return Integer.parseInt(line.substring(start, end).trim());
        } catch (Exception e) { return 0; }
    }

    public static class ScriptExecutionException extends Exception {
        public final int line;
        public ScriptExecutionException(String message, int line) {
            super(message);
            this.line = line;
        }
    }

    private static String parseStringArg(String line) {
        try {
            int start = line.indexOf('(') + 1;
            int end = line.indexOf(')');
            return line.substring(start, end).trim();
        } catch (Exception e) { return ""; }
    }

    private static int generateScriptHash(List<String> script) {
        int h = 0;
        for (String line : script) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue;

            for (char c : trimmed.toCharArray()) {
                h = 31 * h + c;
                h ^= (h << 13);
                h ^= (h >>> 17);
                h ^= (h << 5);
            }
        }
        return Math.abs(h);
    }
}
