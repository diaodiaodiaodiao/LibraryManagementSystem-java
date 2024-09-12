package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ManagerService {

    public static void login() {
        new MainMenuFrame();
    }

    // 主菜单框架
    static class MainMenuFrame extends JFrame {
        public MainMenuFrame() {
            setTitle("diao的图书管理");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(400, 500);
            setLocationRelativeTo(null);
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2, 1));

            // 管理员登录按钮
            JButton adminLoginButton = new JButton("管理员登录");
            adminLoginButton.setFont(adminLoginButton.getFont().deriveFont(Font.PLAIN, 30));
            adminLoginButton.addActionListener(e -> new LoginFrame(this));

            // 读者登录按钮
            JButton readerLoginButton = new JButton("读者登录");
            readerLoginButton.setFont(readerLoginButton.getFont().deriveFont(Font.PLAIN, 30));
            readerLoginButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "作业未要求", "信息", JOptionPane.INFORMATION_MESSAGE));

            panel.add(adminLoginButton);
            panel.add(readerLoginButton);

            getContentPane().add(panel);

            setVisible(true);
        }
    }

    // 登录框架
    static class LoginFrame extends JFrame {
        private final JTextField adminIdField;
        private final JPasswordField passwordField;
        private final JFrame mainMenuFrame;

        public LoginFrame(JFrame mainMenuFrame) {
            this.mainMenuFrame = mainMenuFrame;

            setTitle("管理员登录");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(300, 200);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3, 2));

            // 管理员ID输入框
            panel.add(new JLabel("管理员ID:"));
            adminIdField = new JTextField();
            panel.add(adminIdField);

            // 密码输入框
            panel.add(new JLabel("密码:"));
            passwordField = new JPasswordField();
            panel.add(passwordField);

            // 登录按钮
            JButton loginButton = new JButton("登录");
            loginButton.addActionListener(new LoginActionListener());
            panel.add(loginButton);

            getContentPane().add(panel);

            setVisible(true);
        }

        // 登录按钮的事件监听器
        class LoginActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String adminId = adminIdField.getText();
                String password = new String(passwordField.getPassword());

                // 验证管理员登录信息
                if (ManagerService.validateAdmin(adminId, password)) {
                    mainMenuFrame.dispose();
                    dispose();
                    SwingUtilities.invokeLater(() -> new UpdateAdminInfoFrame(adminId));
                } else {
                    JOptionPane.showMessageDialog(LoginFrame.this, "登录失败，请检查管理员编号和密码！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // 更新管理员信息框架
    static class UpdateAdminInfoFrame extends JFrame {
        public UpdateAdminInfoFrame(String adminId) {
            setTitle("登录成功");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(300, 200);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // 成功登录提示信息
            JLabel messageLabel = new JLabel("登录成功！是否修改管理员信息？", SwingConstants.CENTER);
            panel.add(messageLabel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton yesButton = new JButton("是");
            JButton noButton = new JButton("否");

            yesButton.addActionListener(e -> showUpdateAdminDialog(adminId));
            noButton.addActionListener(e -> {
                dispose();
                SwingUtilities.invokeLater(Main.MainMenuFrame::new);
            });

            buttonPanel.add(yesButton);
            buttonPanel.add(noButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            getContentPane().add(panel);

            setVisible(true);
        }

        // 显示更新管理员信息的对话框
        private void showUpdateAdminDialog(String oldAdminId) {
            JTextField newAdminIdField = new JTextField();
            JTextField newNameField = new JTextField();
            JPasswordField newPasswordField = new JPasswordField();
            JTextField newPhoneNumberField = new JTextField();

            Object[] message = {
                    "新的管理员ID:", newAdminIdField,
                    "新的姓名:", newNameField,
                    "新的密码:", newPasswordField,
                    "新的手机号:", newPhoneNumberField
            };

            int option = JOptionPane.showConfirmDialog(this, message, "更新管理员信息", JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.OK_OPTION) {
                String newAdminId = newAdminIdField.getText().isEmpty() ? null : newAdminIdField.getText();
                String newName = newNameField.getText().isEmpty() ? null : newNameField.getText();
                String newPassword = newPasswordField.getPassword().length == 0 ? null : new String(newPasswordField.getPassword());
                String newPhoneNumber = newPhoneNumberField.getText().isEmpty() ? null : newPhoneNumberField.getText();

                if (newPhoneNumber != null && !DataValidation.isValidTelephone(newPhoneNumber)) {
                    JOptionPane.showMessageDialog(this, "新的手机号格式不正确！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (newAdminId != null || newName != null || newPassword != null || newPhoneNumber != null) {
                    ManagerService.updateAdminInfo(oldAdminId, newAdminId, newName, newPassword, newPhoneNumber);
                    JOptionPane.showMessageDialog(this, "管理员信息更新成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "未做任何更改", "信息", JOptionPane.INFORMATION_MESSAGE);
                }
                dispose();
                SwingUtilities.invokeLater(Main.MainMenuFrame::new);
            } else {
                dispose();
                SwingUtilities.invokeLater(Main.MainMenuFrame::new);
            }
        }
    }

    // 更新管理员信息
    static void updateAdminInfo(String oldAdminId, String newAdminId, String newName, String newPassword, String newPhoneNumber) {
        StringBuilder updateQuery = new StringBuilder("UPDATE manager SET ");
        boolean needsComma = false;
        if (newAdminId != null) {
            updateQuery.append("manager_id = ?");
            needsComma = true;
        }
        if (newName != null) {
            if (needsComma) updateQuery.append(", ");
            updateQuery.append("name = ?");
            needsComma = true;
        }
        if (newPassword != null) {
            if (needsComma) updateQuery.append(", ");
            updateQuery.append("password = ?");
            needsComma = true;
        }
        if (newPhoneNumber != null) {
            if (needsComma) updateQuery.append(", ");
            updateQuery.append("telephone = ?");
        }
        updateQuery.append(" WHERE manager_id = ?");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery.toString())) {

            int paramIndex = 1;
            if (newAdminId != null) {
                stmt.setString(paramIndex++, newAdminId);
            }
            if (newName != null) {
                stmt.setString(paramIndex++, newName);
            }
            if (newPassword != null) {
                stmt.setString(paramIndex++, newPassword);
            }
            if (newPhoneNumber != null) {
                stmt.setString(paramIndex++, newPhoneNumber);
            }
            stmt.setString(paramIndex, oldAdminId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("管理员信息更新成功！");
            } else {
                System.out.println("管理员信息更新失败！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 验证管理员信息
    static boolean validateAdmin(String adminId, String password) {
        String query = "SELECT * FROM manager WHERE manager_id = ? AND password = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, adminId);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
