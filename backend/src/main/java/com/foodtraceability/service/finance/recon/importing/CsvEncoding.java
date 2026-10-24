package com.foodtraceability.service.finance.recon.importing;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * CSV 编码自适应（design §1.3）：UTF-8（含 BOM）优先，失败回退 GB18030（银行导出常见）。
 *
 * <p>判定顺序：</p>
 * <ol>
 *   <li>字节流以 EF BB BF 开头 → UTF-8（去掉 BOM，encodingUsed=UTF-8-BOM）；</li>
 *   <li>严格模式下能按 UTF-8 解码 → UTF-8；</li>
 *   <li>否则按 GB18030 解码（encodingUsed=GB18030）。GB18030 覆盖面广，几乎不会失败，
 *       但**失败时必须整批 FAILED**，不允许 REPLACE 静默替换成 '?'。</li>
 * </ol>
 */
public final class CsvEncoding {

    public static final String UTF8 = "UTF-8";
    public static final String UTF8_BOM = "UTF-8-BOM";
    public static final String GB18030 = "GB18030";

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvEncoding() {
    }

    /** 解码结果：encodingUsed 会写进 import_batches.encoding_used */
    public record Decoded(String text, String encodingUsed) {
    }

    public static Decoded decode(byte[] bytes) {
        if (bytes == null) {
            return new Decoded("", GB18030);
        }
        if (hasBom(bytes)) {
            byte[] body = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, body, 0, body.length);
            return new Decoded(new String(body, StandardCharsets.UTF_8), UTF8_BOM);
        }
        String utf8 = strictDecode(bytes, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return new Decoded(utf8, UTF8);
        }
        Charset gb = Charset.isSupported(GB18030) ? Charset.forName(GB18030) : StandardCharsets.ISO_8859_1;
        String text = strictDecode(bytes, gb);
        if (text != null) {
            return new Decoded(text, GB18030);
        }
        // 两种都失败 → 交给调用方置 PARSED_FAILED（这里返回空串 + 标记编码未知）
        return new Decoded("", "UNKNOWN");
    }

    private static boolean hasBom(byte[] b) {
        return b.length >= 3 && b[0] == BOM[0] && b[1] == BOM[1] && b[2] == BOM[2];
    }

    private static String strictDecode(byte[] bytes, Charset cs) {
        CharsetDecoder dec = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer cb = dec.decode(ByteBuffer.wrap(bytes));
            return cb.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
