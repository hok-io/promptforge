package promptforge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Stops reading before retaining an unbounded stdin stream. */
public final class TaskInputReader {
    public String read(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(16001);
        if (bytes.length > 16000) throw new IllegalArgumentException("stdin exceeds 16000 UTF-8 bytes");
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString().stripTrailing();
        } catch (java.nio.charset.CharacterCodingException error) {
            throw new IllegalArgumentException("stdin must be valid UTF-8");
        }
    }
}
