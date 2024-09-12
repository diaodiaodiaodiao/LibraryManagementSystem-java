package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> ManagerService.login());
    }

    // 主菜单框架
    static class MainMenuFrame extends JFrame {
        public MainMenuFrame() {
            setTitle("主菜单");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(400, 500);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(4, 1, 0, 0));

            String[] options = {
                    "图书信息管理(增删改查)",
                    "读者信息管理(增删改查)",
                    "统计",
                    "退出"
            };

            // 为每个菜单选项创建按钮
            for (String option : options) {
                JButton button = new JButton(option);

                Font currentFont = button.getFont();
                Font newFont = new Font(currentFont.getName(), currentFont.getStyle(), 20);
                button.setFont(newFont);

                // 为按钮添加事件监听器
                button.addActionListener(new MenuActionListener());
                panel.add(button);
            }

            getContentPane().add(panel);

            setVisible(true);
        }

        // 菜单按钮的事件监听器
        class MenuActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();
                String option = button.getText();

                // 根据按钮的文本来执行相应的功能
                switch (option) {
                    case "图书信息管理(增删改查)":
                        BookService.queryAndUpdateBooks();
                        break;
                    case "读者信息管理(增删改查)":
                        ReaderService.showReaderManagementUI();
                        break;
                    case "统计":
                        StatisticsService.showStatisticsUI();
                        break;
                    case "退出":
                        System.out.println("退出系统！");
                        System.exit(0);
                        break;
                    default:
                        JOptionPane.showMessageDialog(MainMenuFrame.this, "无效的选择，请重新选择！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
