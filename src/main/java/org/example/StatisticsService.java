package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatisticsService {

    // 显示统计信息的用户界面
    public static void showStatisticsUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("统计信息");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(400, 500); // 调整界面大小
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(3, 1));

            JButton readerBorrowCountButton = new JButton("统计某位读者的借书数量");
            JButton categoryBookCountButton = new JButton("统计某个类别图书的总藏书量");
            JButton bookBorrowCountButton = new JButton("统计某本书的借阅量");

            Font buttonFont = new Font(readerBorrowCountButton.getFont().getName(), readerBorrowCountButton.getFont().getStyle(), 20); // 设置按钮文本的字体大小为 20

            readerBorrowCountButton.setFont(buttonFont);
            categoryBookCountButton.setFont(buttonFont);
            bookBorrowCountButton.setFont(buttonFont);

            readerBorrowCountButton.addActionListener(e -> showReaderBorrowCount());
            categoryBookCountButton.addActionListener(e -> showCategoryBookCount());
            bookBorrowCountButton.addActionListener(e -> showBookBorrowCount());

            panel.add(readerBorrowCountButton);
            panel.add(categoryBookCountButton);
            panel.add(bookBorrowCountButton);
            frame.add(panel);
            frame.setVisible(true);
        });
    }

    // 显示某位读者的借书数量
    private static void showReaderBorrowCount() {
        String readerNumber = JOptionPane.showInputDialog(null, "请输入读者编号:", "统计借书数量", JOptionPane.PLAIN_MESSAGE);
        if (readerNumber == null || readerNumber.trim().isEmpty()) {
            return; // 用户取消输入或输入为空时直接返回
        }

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT COUNT(*) AS borrow_count FROM borrow_book WHERE reader_number = ?")) {
            preparedStatement.setString(1, readerNumber.trim());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int borrowCount = resultSet.getInt("borrow_count");
                    JOptionPane.showMessageDialog(null, "读者 " + readerNumber + " 借书数量: " + borrowCount);
                } else {
                    JOptionPane.showMessageDialog(null, "未找到该读者的借书记录。");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "查询过程中发生错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 显示某个类别图书的总藏书量
    private static void showCategoryBookCount() {
        String category = JOptionPane.showInputDialog(null, "请输入图书类别:", "统计总藏书量", JOptionPane.PLAIN_MESSAGE);
        if (category == null || category.trim().isEmpty()) {
            return;
        }

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT SUM(book_total) AS total_count FROM book WHERE category = ?")) {
            preparedStatement.setString(1, category.trim());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int totalCount = resultSet.getInt("total_count");
                    JOptionPane.showMessageDialog(null, "类别 " + category + " 总藏书量: " + totalCount);
                } else {
                    JOptionPane.showMessageDialog(null, "未找到该类别的图书记录。");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "查询过程中发生错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 显示某本书的借阅量
    private static void showBookBorrowCount() {
        String bookNumber = JOptionPane.showInputDialog(null, "请输入书号:", "统计借阅量", JOptionPane.PLAIN_MESSAGE);
        if (bookNumber == null || bookNumber.trim().isEmpty()) {
            return;
        }

        try (Connection connection = DBUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT COUNT(*) AS borrow_count FROM borrow_book WHERE book_number = ?")) {
            preparedStatement.setString(1, bookNumber.trim());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int borrowCount = resultSet.getInt("borrow_count");
                    JOptionPane.showMessageDialog(null, "书号 " + bookNumber + " 的借阅量: " + borrowCount);
                } else {
                    JOptionPane.showMessageDialog(null, "未找到该书的借阅记录。");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "查询过程中发生错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
