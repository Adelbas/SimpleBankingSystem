package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.*;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static String url;
    static SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
    static final String SELECT = "SELECT * FROM Card";
    static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS card (\n"
            + "	id integer PRIMARY KEY,\n"
            + "	number text NOT NULL,\n"
            + "	pin text NOT NULL,\n"
            + "	balance integer DEFAULT 0 CHECK(balance>=0)\n"
            + ");";
    static String INSERT_INTO = "INSERT INTO Card (number,pin) VALUES (?,?)";
    static String SELECT_PIN ="SELECT pin FROM Card WHERE number = ?";
    static String SELECT_BALANCE="SELECT balance FROM Card where number = ?";
    static String UPDATE_BALANCE="UPDATE Card SET balance = ? WHERE number = ?";
    static String DELETE="DELETE FROM Card WHERE number = ?";
    static String BIN = "400000";
    static boolean exit = false;
    public static void createTable(){
        try(Connection conn= sqLiteDataSource.getConnection();
            Statement stmt = conn.createStatement()){
            stmt.executeUpdate(CREATE_TABLE);
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }
    public static void insertCard(String cardNum, String pin){
        try (Connection conn = sqLiteDataSource.getConnection()) {
             PreparedStatement preparedStatement = conn.prepareStatement(INSERT_INTO);
             preparedStatement.setString(1,cardNum);
             preparedStatement.setString(2,pin);
             preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    public static void main(String[] args) {
//        url = "jdbc:sqlite:" + args[1];
        url = "jdbc:sqlite:card.s3db";
        sqLiteDataSource.setUrl(url);
        createTable();
        while(!exit){
            showMainMenu();
            switch (sc.nextInt()){
                case 1:
                    createAccount();
                    break;
                case 2:
                    logIn();
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    System.out.println("Please, try again");
            }
        }
        System.out.println("Bye!");
    }
    public static void showMainMenu(){
        System.out.println("1. Create an account\n2. Log into account\n0. Exit");
    }

    public static String createNewPin(){
        int max = 9999;
        Random rnd = new Random();
        return String.format("%04d",rnd.nextInt(max+1));
    }
    public static String luchnAlgorithm(String accId){
        int[]mas = new int[accId.length()];
        int sum = 0;
        for (int i=0; i<mas.length; i++) {
            mas[i] = Character.getNumericValue(accId.charAt(i));
            if (i%2==0) {
                mas[i] *= 2;
                if(mas[i]>9)
                    mas[i]-=9;
            }
            sum+=mas[i];
        }
        int k = 0;
        while(sum%10!=0){
            k++;
            sum+=1;
        }
        return String.valueOf(k);
    }
    public static String createNewCardNum(){
        int max = 999999999;
        Random rnd = new Random();
        String accountIdentifier = String.format("%09d",rnd.nextInt(max+1));
        String checksum = luchnAlgorithm(BIN+accountIdentifier);
        return BIN+accountIdentifier+checksum;
    }

    public static boolean checkCardNum(String cardNum){
        try(Connection conn = sqLiteDataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SELECT)){
            while(rs.next()){
                String tmp = rs.getString("number");
                if (cardNum.equals(tmp))
                    return true;
            }
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static void createAccount(){
        String pin = createNewPin();
        String cardNum = createNewCardNum();
        while(checkCardNum(cardNum))
            cardNum = createNewCardNum();
        insertCard(cardNum, pin);
        System.out.println("Your card has been created\n" +
                "Your card number:\n" +
                cardNum +
                "\nYour card PIN:\n" +
                pin);
    }

    public static boolean authentication(String cardNum, String pin){
        try(Connection conn = sqLiteDataSource.getConnection()){
            PreparedStatement preparedStatement = conn.prepareStatement(SELECT_PIN);
            preparedStatement.setString(1,cardNum);
            try(ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next() && pin.equals(rs.getString("pin")))
                    return true;
            }
            catch (SQLException e2){
                System.out.println(e2.getMessage());
            }
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return false;
    }

    public static void logIn(){
        System.out.println("Enter your card number:");
        String cardNum = sc.next();
        System.out.println("Enter your PIN:");
        String pin = sc.next();
        if (authentication(cardNum, pin)){
            System.out.println("\nYou have successfully logged in!\n");
            userInterface(cardNum);
        }
        else System.out.println("\nWrong card number or PIN!\n");
    }
    public static void showInsideUI(){
        System.out.println("1. Balance\n" +
                "2. Add income\n" +
                "3. Do transfer\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit");
    }

    public static int getBalance(String cardNum){
        try(Connection conn = sqLiteDataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(SELECT_BALANCE)){
            preparedStatement.setString(1,cardNum);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.getInt("balance");
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        return -1;
    }
    public static void addIncome(String cardNum){
        System.out.println("\nEnter income:");
        int in = sc.nextInt();
        if (in<0){
            System.out.println("Invalid sum!\n");
            return;
        }
        try (Connection conn = sqLiteDataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(UPDATE_BALANCE)){
            int sum = in+getBalance(cardNum);
            preparedStatement.setString(1, String.valueOf(sum));
            preparedStatement.setString(2, cardNum);
            preparedStatement.executeUpdate();
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
        System.out.println("Income was added!\n");
    }
    public static void doTransfer(String cardNum,int balance, String recNum, int sum){
        try(Connection conn = sqLiteDataSource.getConnection()){
            conn.setAutoCommit(false);
            try(PreparedStatement updateSender = conn.prepareStatement(UPDATE_BALANCE);
                PreparedStatement updateRec = conn.prepareStatement(UPDATE_BALANCE)){
                updateSender.setInt(1,balance-sum);
                updateSender.setString(2,cardNum);
                updateRec.setInt(1,getBalance(recNum)+sum);
                updateRec.setString(2,recNum);
                updateSender.executeUpdate();
                updateRec.executeUpdate();
                conn.commit();
                System.out.println("Success!\n");
            }
            catch (SQLException e2){
                try {
                    System.err.println("Transaction is being rolled back\n");
                    conn.rollback();
                } catch (SQLException excep) {
                    excep.printStackTrace();
                }
            }
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
    public static void transferMenu(String cardNum){
        System.out.println("\nTransfer\n" +
                "Enter card number:");
        String recNum = sc.next();
        if (recNum.equals(cardNum)){
            System.out.println("You can't transfer money to the same account!\n");
        }
        else if(recNum.length() != 0 && !luchnAlgorithm(recNum.substring(0,recNum.length()-1)).equals(String.valueOf(recNum.charAt(recNum.length()-1)))){
            System.out.println("Probably you made a mistake in the card number. Please try again!\n");
        }
        else if(checkCardNum(recNum)){
            System.out.println("Enter how much money you want to transfer:");
            int sum = sc.nextInt();
            int balance = getBalance(cardNum);
            if (sum>=0 && sum<=balance)
                doTransfer(cardNum,balance,recNum,sum);
            else if (sum<0) System.out.println("Invalid sum!\n");
            else System.out.println("Not enough money!\n");
        }
        else System.out.println("Such a card does not exist.\n");
    }
    public static void closeAccount(String cardNum){
        try(Connection conn = sqLiteDataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(DELETE)){
            preparedStatement.setString(1,cardNum);
            preparedStatement.executeUpdate();
            System.out.println("\nThe account has been closed!\n");
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
    public static void userInterface(String cardNum){
        boolean logOut = false;
        while(!logOut && !exit){
            showInsideUI();
            switch (sc.nextInt()){
                case 1:
                    System.out.println("\nBalance: "+getBalance(cardNum)+"\n");
                    break;
                case 2:
                    addIncome(cardNum);
                    break;
                case 3:
                    transferMenu(cardNum);
                    break;
                case 4:
                    closeAccount(cardNum);
                    logOut=true;
                    break;
                case 5:
                    System.out.println("\nYou have successfully logged out!\n");
                    logOut=true;
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    System.out.println("Please, try again");

            }
        }

    }

}
//class Card{
//    private String cardNum;
//    private String pin;
//    private int balance = 0;
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Card card = (Card) o;
//        return cardNum.equals(card.cardNum);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(cardNum);
//    }
//
//
//    public Card(String cardNum, String pin) {
//        this.cardNum = cardNum;
//        this.pin = pin;
//    }
//
//    public String getCardNum() {
//        return cardNum;
//    }
//
//    public void setCardNum(String cardNum) {
//        this.cardNum = cardNum;
//    }
//
//    public String getPin() {
//        return pin;
//    }
//
//    public void setPin(String pin) {
//        this.pin = pin;
//    }
//
//    public int getBalance() {
//        return balance;
//    }
//
//    public void setBalance(int balance) {
//        this.balance = balance;
//    }
//}