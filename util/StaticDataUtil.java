package calypsox.tk.engine.util;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.calypso.tk.core.sql.ioSQL;

public class StaticDataUtil extends ioSQL implements Serializable {

     
    Connection connection;

    public StaticDataUtil() throws SQLException {
        connection = ioSQL.getConnection();
    }

    public void getTraderNames() {
        String sql = "SELECT * FROM DOMAIN_VALUES where NAME='trader'";

    }

    public void getSalesPerson() {
        String sql = "SELECT * FROM DOMAIN_VALUES where NAME='salesPerson'";
    }

    public void getCurrency() {
        String sql = "SELECT * FROM DOMAIN_VALUES where NAME='Currency'";

    }

    public String getProduct() {
        String sql = "SELECT PRODUCT_BOND.PRODUCT_ID, PRODUCT_BOND.BOND_NAME, PRODUCT_BOND.CURRENCY, PRODUCT_BOND.ISSUE_DATE, \n" +
                "PRODUCT_BOND.MATURITY_DATE, PRODUCT_BOND.DAYCOUNT, PRODUCT_BOND.MIN_PURCHASE_AMT, PRODUCT_BOND.BOND_TYPE,\n" +
                "LEGAL_ENTITY.LONG_NAME FROM PRODUCT_BOND  join LEGAL_ENTITY on PRODUCT_BOND.ISSUER_LE_ID = LEGAL_ENTITY.LEGAL_ENTITY_ID";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Id,BondName,CurrencyId,IssueDate,MaturityDate,DayCount,MinimumPurshaseAmount,ProductType,ProcessingOrg\n");

        try {

            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery(sql);

            String line = null;

            while (result.next()) {
                String Id = result.getString("PRODUCT_ID");
                String BondName = result.getString("BOND_NAME");
                String CurrencyId = result.getString("CURRENCY");
                String IssueDate = result.getString("ISSUE_DATE");
                String MaturityDate = result.getString("MATURITY_DATE");
                String DayCount = result.getString("DAYCOUNT");
                String MinimumPurshaseAmount = result.getString("MIN_PURCHASE_AMT");
                String ProductType = result.getString("BOND_TYPE");
                String ProcessingOrg = result.getString("LONG_NAME");


                line = String.format("\"%s\",%s,%s,%s,%s,%s,%s,%s,%s\n",
                        Id, BondName, CurrencyId, IssueDate, MaturityDate, DayCount, MinimumPurshaseAmount, ProductType, ProcessingOrg);
                stringBuilder.append(line);
//                currency_rule, Acc_holiday_list, Adjustment_days, Acc_rule_type


            }

            statement.close();
            return stringBuilder.toString();


        } catch (SQLException e) {
            System.out.println("Datababse error:");
            e.printStackTrace();
        }

        return null;


    }

    public String getHolidayCode() {
        String sql = "SELECT HOLIDAY_CODE.HOLIDAY_CODE FROM HOLIDAY_CODE";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HOLIDAY_CODE\n");

        try {

            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery(sql);

            String line = null;

            while (result.next()) {
                String holidayCode = result.getString("HOLIDAY_CODE");


                line = String.format("\"%s\"\n",
                        holidayCode);
                stringBuilder.append(line);
//                currency_rule, Acc_holiday_list, Adjustment_days, Acc_rule_type


            }

            statement.close();
            return stringBuilder.toString();


        } catch (SQLException e) {
            System.out.println("Datababse error:");
            e.printStackTrace();
        }

        return null;

    }

}


