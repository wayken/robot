package cloud.apposs.robot.shellx.tui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * TUI 输入处理器，负责读取用户输入，支持多行模式和管道输入
 */
public class TuiInput {
    private final BufferedReader reader;
    private final boolean interactive;

    public TuiInput() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.interactive = System.console() != null;
    }

    /**
     * 读取一行用户输入
     *
     * @return 用户输入内容，null 表示 EOF
     */
    public String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * 读取管道中的所有输入
     *
     * @return 完整输入内容
     */
    public String readPipeInput() throws IOException {
        if (interactive) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 读取多行输入（直到用户输入空行为止）
     *
     * @return 多行输入内容
     */
    public String readMultiLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void close() throws IOException {
        reader.close();
    }
}
