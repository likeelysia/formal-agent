package io.github.likeelysia.formalagent.cli;

import io.github.likeelysia.formalagent.config.SpringConfig;
import io.github.likeelysia.formalagent.exception.AgentException;
import io.github.likeelysia.formalagent.service.ChatService;
import io.github.likeelysia.formalagent.store.SessionStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // ① 启动 Spring 容器:它会自己造好 DeepSeekClient → ChatSession → ChatService
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(SpringConfig.class);

        // ② 一行拿到"已经装配好"的服务(以前要手工 new 两行)
        ChatService service = ctx.getBean(ChatService.class);

        // ③ 循环:和以前一模一样
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你 > ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().strip();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            try {
                System.out.println("AI > " + service.send(line));
            } catch (AgentException e) {
                System.out.println("[出错] " + e.getMessage());
            }
        }

        // ④ 存档 + 关容器
        SessionStore.save(service.getSession());
        SessionStore.exportJson(service.getSession());
        scanner.close();
        ctx.close();                       // 容器关掉,里面单例对象统一销毁
    }
}