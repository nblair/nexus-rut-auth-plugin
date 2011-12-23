/*

MD5Crypt.java

Created: 3 November 1999
Release: $Name:  $
Version: $Revision: 7678 $
Last Mod Date: $Date: 2007-12-28 11:51:49 -0600 (Fri, 28 Dec 2007) $
Java Port By: Jonathan Abbey, jonabbey@arlut.utexas.edu
Original C Version:
----------------------------------------------------------------------------
"THE BEER-WARE LICENSE" (Revision 42):
<phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
can do whatever you want with this stuff. If we meet some day, and you think
this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
----------------------------------------------------------------------------

This Java Port is  

  Copyright (c) 1999-2008 The University of Texas at Austin.

  All rights reserved.

  Redistribution and use in source and binary form are permitted
  provided that distributions retain this entire copyright notice
  and comment. Neither the name of the University nor the names of
  its contributors may be used to endorse or promote products
  derived from this software without specific prior written
  permission. THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY
  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE.

*/

package edu.wisc.nexus.auth.rut.dao;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/*------------------------------------------------------------------------------
                                                                        class
                                                                     MD5Crypt

------------------------------------------------------------------------------*/

/**
* This class defines a method,
* {@link MD5Crypt#crypt(java.lang.String, java.lang.String) crypt()}, which
* takes a password and a salt string and generates an OpenBSD/FreeBSD/Linux-compatible
* md5-encoded password entry.
*/

public final class MD5Crypt {
    public enum CryptType {
        CRYPT("1"),
        APR1("apr1");
        
        private final String prefix;
        
        private CryptType(String prefix) {
            this.prefix = FIELD_SEPERATOR + prefix + FIELD_SEPERATOR;
        }

        public String getPrefix() {
            return prefix;
        }
    }
    
    private static final char FIELD_SEPERATOR = '$';
    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final String BASE64_CHARS = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Random RND = new SecureRandom();
    private static int[][] TO64_BYTE_ORDER = { {0, 6, 12}, {1, 7, 13}, {2, 8, 14}, {3, 9, 15}, {4, 10, 5} };

    /**
    * convert an encoded unsigned byte value into a int
    * with the unsigned value.
    */
    private static final int bytes2u(byte inp) {
        return inp & 0xff;
    }
    
    /**
     * Converts a long into the specified number of BASE64 characters 6 bits at a time
     */
    private static final void to64(long v, int size, final StringBuilder result) {
        while (--size >= 0) {
            result.append(BASE64_CHARS.charAt((int) (v & 0x3f)));
            v >>>= 6;
        }
    }
    
    /**
     * Convert a byte[] into a base64 encoded string
     */
    private static void to64(final byte[] finalState, final StringBuilder result) {
        long l;
        
        for (final int[] byteOrder : TO64_BYTE_ORDER) {
            //Builds 3 bytes into 1 long
            l = bytes2u(finalState[byteOrder[0]]) << 16 | 
                    bytes2u(finalState[byteOrder[1]]) << 8 | 
                    bytes2u(finalState[byteOrder[2]]);
            to64(l, 4, result);
        }
        
        l = bytes2u(finalState[11]);
        to64(l, 2, result);
    }

    private static MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("MD5");
        }
        catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a random string from the valid salt characters
     */
    public static String createRandomSalt(int length) {
        final char[] saltChars = new char[length];

        for (int i = 0; i < saltChars.length; i++) {
            final int index = RND.nextInt(SALT_CHARS.length());
            saltChars[i] = SALT_CHARS.charAt(index);
        }
        
        return new String(saltChars);
    }

    /**
     * <p>This method actually generates a OpenBSD/FreeBSD/Linux PAM compatible
     * md5-encoded password hash from a plaintext password and a
     * salt.</p>
     *
     * <p>The resulting string will be in the form '$1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
     *
     * @param password Plaintext password
     *
     * @return An OpenBSD/FreeBSD/Linux-compatible md5-hashed password field.
     */
    public static final String crypt(String password) {
        final String salt = createRandomSalt(8);
        
        return MD5Crypt.crypt(password, salt);
    }

    /**
    * <p>This method actually generates a OpenBSD/FreeBSD/Linux PAM compatible
    * md5-encoded password hash from a plaintext password and a
    * salt.</p>
    *
    * <p>The resulting string will be in the form '$1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
    *
    * @param password Plaintext password
    * @param salt A short string to use to randomize md5.  May start with $1$, which
    *             will be ignored.  It is explicitly permitted to pass a pre-existing
    *             MD5Crypt'ed password entry as the salt.  crypt() will strip the salt
    *             chars out properly.
    *
    * @return An OpenBSD/FreeBSD/Linux-compatible md5-hashed password field.
    */
    public static final String crypt(String password, String salt) {
        return MD5Crypt.crypt(password, salt, CryptType.CRYPT);
    }

    /**
    * <p>This method generates an Apache MD5 compatible
    * md5-encoded password hash from a plaintext password and a
    * salt.</p>
    *
    * <p>The resulting string will be in the form '$apr1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
    *
    * @param password Plaintext password
    *
    * @return An Apache-compatible md5-hashed password string.
    */

    public static final String apacheCrypt(String password) {
        final String salt = createRandomSalt(8);
        return MD5Crypt.apacheCrypt(password, salt);
    }

    /**
    * <p>This method actually generates an Apache MD5 compatible
    * md5-encoded password hash from a plaintext password and a
    * salt.</p>
    *
    * <p>The resulting string will be in the form '$apr1$&lt;salt&gt;$&lt;hashed mess&gt;</p>
    *
    * @param password Plaintext password
    * @param salt A short string to use to randomize md5.  May start with $apr1$, which
    *             will be ignored.  It is explicitly permitted to pass a pre-existing
    *             MD5Crypt'ed password entry as the salt.  crypt() will strip the salt
    *             chars out properly.
    *
    * @return An Apache-compatible md5-hashed password string.
    */

    public static final String apacheCrypt(String password, String salt) {
        return MD5Crypt.crypt(password, salt, CryptType.APR1);
    }

    /**
    * <p>This method actually generates md5-encoded password hash from
    * a plaintext password, a salt, and a magic string.</p>
    *
    * <p>There are two magic strings that make sense to use here.. '$1$' is the
    * magic string used by the FreeBSD/Linux/OpenBSD MD5Crypt algorithm, and
    * '$apr1$' is the magic string used by the Apache MD5Crypt algorithm.</p>
    *
    * <p>The resulting string will be in the form '&lt;magic&gt;&lt;salt&gt;$&lt;hashed mess&gt;</p>
    *
    * @param password Plaintext password
    * @param salt A short string to use to randomize md5.  May start
    * with the magic string, which will be ignored.  It is explicitly
    * permitted to pass a pre-existing MD5Crypt'ed password entry as
    * the salt.  crypt() will strip the salt chars out properly.
    * @param magic Either "$apr1$" or "$1$", which controls whether we
    * are doing Apache-style or FreeBSD-style md5Crypt.
    * 
    * @return An md5-hashed password string. 
    */
    public static final String crypt(String password, String salt, CryptType cryptType) {
        /* Refine the Salt first */

        /* If it starts with the magic string, then skip that */

        final String prefix = cryptType.getPrefix();
        if (salt.startsWith(prefix)) {
            salt = salt.substring(prefix.length());
        }

        /* It stops at the first '$', max 8 chars */

        if (salt.indexOf(FIELD_SEPERATOR) != -1) {
            salt = salt.substring(0, salt.indexOf(FIELD_SEPERATOR));
        }

        if (salt.length() > 8) {
            salt = salt.substring(0, 8);
        }

        final MessageDigest ctx = getMD5();

        ctx.update(password.getBytes()); // The password first, since that is what is most unknown
        ctx.update(prefix.getBytes()); // Then our magic string
        ctx.update(salt.getBytes()); // Then the raw salt

        /* Then just as many characters of the MD5(pw,salt,pw) */

        final MessageDigest ctx1 = getMD5();
        ctx1.update(password.getBytes());
        ctx1.update(salt.getBytes());
        ctx1.update(password.getBytes());
        
        
        byte[] finalState = ctx1.digest();

        for (int pl = password.length(); pl > 0; pl -= 16) {
            ctx.update(finalState, 0, pl > 16 ? 16 : pl);
        }

        /* the original code claimed that finalState was being cleared
           to keep dangerous bits out of memory, but doing this is also
           required in order to get the right output. */

        Arrays.fill(finalState, (byte) 0);

        /* Then something really weird... */

        for (int i = password.length(); i != 0; i >>>= 1) {
            if ((i & 1) != 0) {
                ctx.update(finalState, 0, 1);
            }
            else {
                ctx.update(password.getBytes(), 0, 1);
            }
        }

        finalState = ctx.digest();

        /*
         * and now, just to make sure things don't run too fast
         * On a 60 Mhz Pentium this takes 34 msec, so you would
         * need 30 seconds to build a 1000 entry dictionary...
         *
         * (The above timings from the very old C version)
         */
        for (int i = 0; i < 1000; i++) {
            ctx1.reset();

            if ((i & 1) != 0) {
                ctx1.update(password.getBytes());
            }
            else {
                ctx1.update(finalState, 0, 16);
            }

            if (i % 3 != 0) {
                ctx1.update(salt.getBytes());
            }

            if (i % 7 != 0) {
                ctx1.update(password.getBytes());
            }

            if ((i & 1) != 0) {
                ctx1.update(finalState, 0, 16);
            }
            else {
                ctx1.update(password.getBytes());
            }

            finalState = ctx1.digest();
        }

        /* Now make the output string */

        final StringBuilder result = new StringBuilder(prefix.length() + salt.length() + 1 + 22);

        result.append(prefix);
        result.append(salt);
        result.append(FIELD_SEPERATOR);

        to64(finalState, result);

        return result.toString();
    }

    /**
     * This method tests a plaintext password against a md5Crypt'ed hash and returns
     * true if the password matches the hash.
     *
     * This method will work properly whether the hashtext was crypted
     * using the default FreeBSD md5Crypt algorithm or the Apache
     * md5Crypt variant.
     *
     * @param plaintextPass The plaintext password text to test.
     * @param md5CryptText The Apache or FreeBSD-md5Crypted hash used to authenticate the plaintextPass.
     */
    public static final boolean verifyPassword(String plaintextPass, String md5CryptText) {
        if (md5CryptText.startsWith(CryptType.CRYPT.getPrefix())) {
            return md5CryptText.equals(MD5Crypt.crypt(plaintextPass, md5CryptText));
        }
        else if (md5CryptText.startsWith(CryptType.APR1.getPrefix())) {
            return md5CryptText.equals(MD5Crypt.apacheCrypt(plaintextPass, md5CryptText));
        }
        else {
            throw new RuntimeException("Bad md5CryptText");
        }
    }
}