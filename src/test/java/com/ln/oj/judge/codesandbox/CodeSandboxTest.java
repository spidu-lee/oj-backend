package com.ln.oj.judge.codesandbox;

import com.ln.oj.core.codesandbox.CodeSandbox;
import com.ln.oj.core.codesandbox.CodeSandboxFactory;
import com.ln.oj.core.codesandbox.CodeSandboxProxy;
import com.ln.oj.core.codesandbox.model.ExecuteCodeRequest;
import com.ln.oj.core.codesandbox.model.ExecuteCodeResponse;
import com.ln.oj.model.enums.QuestionSubmitLanguageEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author ln
 */
@SpringBootTest
class CodeSandboxTest {
    @Value("${codeSandbox.type:example}")
    private String type;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String type = scanner.next();
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            String code = "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        int a = Integer.parseInt(args[0]);\n" +
                    "        int b = Integer.parseInt(args[1]);\n" +
                    "        System.out.println(\"结果:\" + (a + b));\n" +
                    "    }\n" +
                    "}";
            String language = QuestionSubmitLanguageEnum.JAVA.getValue();
            List<String> inputList = Arrays.asList("1 2","3 4");
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .inputList(inputList)
                    .language(language)
                    .build();
            ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
            System.out.println(executeCodeResponse);
        }
    }

}