/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. 
 * See the copyright.txt in the distribution for a full listing 
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 * 
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
/*
 * Copyright (C) 2003, 2004
 * Arjuna Technologies Limited
 * Newcastle upon Tyne, UK
 *
 * $Id: Account.java 2342 2006-03-30 13:06:17Z  $
 */
package com.arjuna.demo.jts.localbank;

import com.arjuna.ats.jts.OTSManager;
import com.arjuna.orbportability.OA;
import com.arjuna.orbportability.ORB;
import org.omg.CosTransactions.Inactive;
import org.omg.CosTransactions.RecoveryCoordinator;
import org.omg.CosTransactions.Resource;
import org.omg.CosTransactions.Unavailable;

/**
 * The account object reflects a non-persistent transaction-aware representation of a bank account. As a bank account,
 * it has a balance associated with it. As a transaction-aware entity it has a trailmap defined ResourcePOA associated
 * with it (a single ResourcePOA is created for each transaction that uses the account).
 *
 * This object allows an account balance to be interrogated, increased or decreased, all under transactional control.
 *
 * For this trailmap example the resource is enslisted with a transaction using implicit access to the transactions
 * coordinator.
 *
 * The account object may only be accessed locally, even though its resource may be accessed remotely.
 */
public class Account
{
    /**
     * The current value of the bank account.
     */
    float _balance;

    /**
     * The name of the bank account holder.
     */
    private String _name;

    /**
     * The transactional update entity associated with this account.
     */
    AccountResource accRes = null;

    /**
     * A CORBA reference to the account resource suitable for registration with the transaction coordinator.
     */
    private Resource ref;

    /**
     * Create a new zerioed bank account and associate a name with it.
     *
     * @param name  The name of the bank account holder.
     */

    public Account(String name)
    {
        _name = name;
        _balance = 0;
    }

    /**
     * Get the current balance of the account within the scope of the current transaction. The transaction control to
     * indicate the transaction to acquire the balance under is implictly accessed.
     *
     * @return  The balance as it stands for this transaction.
     */
    public float balance()
    {
        return getResource().balance();
    }

    /**
     * Credit this account under control of the current transaction. The transaction control to indicate the transaction
     * to update the account under is implicitly accessed.
     *
     * @param value The amount to increase the account balance by.
     */
    public void credit(float value)
    {
        getResource().credit(value);
    }

    /**
     * Debit this account under control of the current transaction. The transaction control to use is implicitly accessed
     * to update the account under.
     *
     * @param value The amount to decrease the account balance by.
     */
    public void debit(float value)
    {
        getResource().debit(value);
    }


    /**
     * Get and enlist the account resource ready for access. The transaction to enlist the resource within is implicitly
     * identified.
     *
     * @return          A ResourcePOA which has been enlisted in the current transaction.
     */
    private AccountResource getResource()
    {
        if (accRes == null)
        {
            accRes = new AccountResource(this, _name);
            // The resource reference is obtained so that we can register the resource within the transaction
            // This trailmap's account resource will be accessed from the ORB instance "test"
            ref = org.omg.CosTransactions.ResourceHelper.narrow(OA.getRootOA(ORB.getInstance("test")).corbaReference(accRes));
            try
            {
                RecoveryCoordinator recoverycoordinator = OTSManager.get_current().get_control().get_coordinator().register_resource(ref);
            }
            catch (Inactive inactive)
            {
                // Display as much help as possible to the user track down the configuration problem
                System.err.println("CosTransactions Error: Could not register_resource() Inactive exception: " + inactive);
                inactive.printStackTrace();
                System.exit(0);
            }
            catch (Unavailable unavailable)
            {
                // Display as much help as possible to the user track down the configuration problem
                System.err.println("CosTransactions Error: Could not get_coordinator() Unavailable exception: " + unavailable);
                unavailable.printStackTrace();
                System.exit(0);
            }
            catch (org.omg.CORBA.SystemException systemException)
            {
                // Display as much help as possible to the user track down the configuration problem
                System.err.println("CosTransactions Error: Could not get_current() SystemException: " + systemException);
                systemException.printStackTrace();
                System.exit(0);
            }
        }
        return accRes;
    }
}
