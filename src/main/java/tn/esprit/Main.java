package tn.esprit;

import java.sql.Connection;

import tn.esprit.utiles.MyDB;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        System.out.printf("Hello and welcome!");
        MyDB db1 = MyDB.getInstance();
        Connection conn = MyDB.getInstance().getConnection();
    }
}