/******************************************************************************
 * Copyright © 2013-2016 The kpl Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * kpl software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package kpl.http;


import kpl.Account;
import kpl.Alias;
import kpl.Attachment;
import kpl.Constants;
import kpl.kplException;
import kpl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static kpl.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static kpl.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static kpl.http.JSONResponses.INCORRECT_URI_LENGTH;
import static kpl.http.JSONResponses.MISSING_ALIAS_NAME;

public final class SetAlias extends CreateTransaction {

    static final SetAlias instance = new SetAlias();

    private SetAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "aliasName", "aliasURI");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws kplException {
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        String aliasURI = Convert.nullToEmpty(req.getParameter("aliasURI"));

        if (aliasName == null) {
            return MISSING_ALIAS_NAME;
        }

        aliasName = aliasName.trim();
        if (aliasName.length() == 0 || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = aliasName.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS_NAME;
            }
        }

        aliasURI = aliasURI.trim();
        if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
        }

        Account account = ParameterParser.getSenderAccount(req);

        Alias alias = Alias.getAlias(normalizedAlias);
        if (alias != null && alias.getAccountId() != account.getId()) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 8);
            response.put("errorDescription", "\"" + aliasName + "\" is already used");
            return response;
        }

        Attachment attachment = new Attachment.MessagingAliasAssignment(aliasName, aliasURI);
        return createTransaction(req, account, attachment);

    }

}
