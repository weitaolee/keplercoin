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

package kpl;

public final class VoteWeighting {

    public enum VotingModel {
        NONE(-1) {
            @Override
            public final boolean acceptsVotes() {
                return false;
            }
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                throw new UnsupportedOperationException("No voting possible for VotingModel.NONE");
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.NONE;
            }
        },
        ACCOUNT(0) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                return (voteWeighting.minBalance == 0 || voteWeighting.minBalanceModel.getBalance(voteWeighting, voterId, height) >= voteWeighting.minBalance) ? 1 : 0;
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.NONE;
            }
        },
        NQT(1) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long nqtBalance = Account.getAccount(voterId, height).getBalanceNQT();
                return nqtBalance >= voteWeighting.minBalance ? nqtBalance : 0;
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.NQT;
            }
        },
        ASSET(2) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long qntBalance = Account.getAssetBalanceQNT(voterId, voteWeighting.holdingId, height);
                return qntBalance >= voteWeighting.minBalance ? qntBalance : 0;
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.ASSET;
            }
        },
        CURRENCY(3) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                long units = Account.getCurrencyUnits(voterId, voteWeighting.holdingId, height);
                return units >= voteWeighting.minBalance ? units : 0;
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.CURRENCY;
            }
        },
        TRANSACTION(4) {
            @Override
            public final boolean acceptsVotes() {
                return false;
            }
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                throw new UnsupportedOperationException("No voting possible for VotingModel.TRANSACTION");
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.NONE;
            }
        },
        HASH(5) {
            @Override
            public final long calcWeight(VoteWeighting voteWeighting, long voterId, int height) {
                return 1;
            }
            @Override
            public final MinBalanceModel getMinBalanceModel() {
                return MinBalanceModel.NONE;
            }
        };

        private final byte code;

        VotingModel(int code) {
            this.code = (byte)code;
        }

        public byte getCode() {
            return code;
        }

        public abstract long calcWeight(VoteWeighting voteWeighting, long voterId, int height);

        public abstract MinBalanceModel getMinBalanceModel();

        public boolean acceptsVotes() {
            return true;
        }

        public static VotingModel get(byte code) {
            for (VotingModel votingModel : values()) {
                if (votingModel.getCode() == code) {
                    return votingModel;
                }
            }
            throw new IllegalArgumentException("Invalid votingModel " + code);
        }
    }

    public enum MinBalanceModel {
        NONE(0) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                throw new UnsupportedOperationException();
            }
        },
        NQT(1) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getAccount(voterId, height).getBalanceNQT();
            }
        },
        ASSET(2) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getAssetBalanceQNT(voterId, voteWeighting.holdingId, height);
            }
        },
        CURRENCY(3) {
            @Override
            public final long getBalance(VoteWeighting voteWeighting, long voterId, int height) {
                return Account.getCurrencyUnits(voterId, voteWeighting.holdingId, height);
            }
        };

        private final byte code;

        MinBalanceModel(int code) {
            this.code = (byte)code;
        }

        public byte getCode() {
            return code;
        }

        public abstract long getBalance(VoteWeighting voteWeighting, long voterId, int height);

        public static MinBalanceModel get(byte code) {
            for (MinBalanceModel minBalanceModel : values()) {
                if (minBalanceModel.getCode() == code) {
                    return minBalanceModel;
                }
            }
            throw new IllegalArgumentException("Invalid minBalanceModel " + code);
        }
    }

    private final VotingModel votingModel;
    private final long holdingId; //either asset id or MS coin id
    private final long minBalance;
    private final MinBalanceModel minBalanceModel;


    public VoteWeighting(byte votingModel, long holdingId, long minBalance, byte minBalanceModel) {
        this.votingModel = VotingModel.get(votingModel);
        this.holdingId = holdingId;
        this.minBalance = minBalance;
        this.minBalanceModel = MinBalanceModel.get(minBalanceModel);
    }

    public VotingModel getVotingModel() {
        return votingModel;
    }

    public long getMinBalance() {
        return minBalance;
    }

    public long getHoldingId() {
        return holdingId;
    }

    public MinBalanceModel getMinBalanceModel() {
        return minBalanceModel;
    }

    public void validate() throws kplException.ValidationException {
        if (votingModel == null) {
            throw new kplException.NotValidException("Invalid voting model");
        }
        if (minBalanceModel == null) {
            throw new kplException.NotValidException("Invalid min balance model");
        }
        if ((votingModel == VotingModel.ASSET || votingModel == VotingModel.CURRENCY) && holdingId == 0) {
            throw new kplException.NotValidException("No holdingId provided");
        }
        if (votingModel == VotingModel.CURRENCY && Currency.getCurrency(holdingId) == null) {
            throw new kplException.NotCurrentlyValidException("Currency " + Long.toUnsignedString(holdingId) + " not found");
        }
        if (votingModel == VotingModel.ASSET && Asset.getAsset(holdingId) == null) {
            throw new kplException.NotCurrentlyValidException("Asset " + Long.toUnsignedString(holdingId) + " not found");
        }
        if (minBalance < 0) {
            throw new kplException.NotValidException("Invalid minBalance " + minBalance);
        }
        if (minBalance > 0) {
            if (minBalanceModel == MinBalanceModel.NONE) {
                throw new kplException.NotValidException("Invalid min balance model " + minBalanceModel);
            }
            if (votingModel.getMinBalanceModel() != MinBalanceModel.NONE && votingModel.getMinBalanceModel() != minBalanceModel) {
                throw new kplException.NotValidException("Invalid min balance model: " + minBalanceModel + " for voting model " + votingModel);
            }
            if ((minBalanceModel == MinBalanceModel.ASSET || minBalanceModel == MinBalanceModel.CURRENCY) && holdingId == 0) {
                throw new kplException.NotValidException("No holdingId provided");
            }
            if (minBalanceModel == MinBalanceModel.ASSET && Asset.getAsset(holdingId) == null) {
                throw new kplException.NotCurrentlyValidException("Invalid min balance asset: " + Long.toUnsignedString(holdingId));
            }
            if (minBalanceModel == MinBalanceModel.CURRENCY && Currency.getCurrency(holdingId) == null) {
                throw new kplException.NotCurrentlyValidException("Invalid min balance currency: " + Long.toUnsignedString(holdingId));
            }
        }
        if (minBalance == 0 && votingModel == VotingModel.ACCOUNT && holdingId != 0) {
            throw new kplException.NotValidException("HoldingId cannot be used in by account voting with no min balance");
        }
        if ((votingModel == VotingModel.NQT || minBalanceModel == MinBalanceModel.NQT) && holdingId != 0) {
            throw new kplException.NotValidException("HoldingId cannot be used in by balance voting or with min balance in NQT");
        }
        if ((!votingModel.acceptsVotes() || votingModel == VotingModel.HASH) && (holdingId != 0 || minBalance != 0 || minBalanceModel != MinBalanceModel.NONE)) {
            throw new kplException.NotValidException("With VotingModel " + votingModel + " no holdingId, minBalance, or minBalanceModel should be specified");
        }
    }

    public boolean isBalanceIndependent() {
        return (votingModel == VotingModel.ACCOUNT && minBalance == 0) || !votingModel.acceptsVotes() || votingModel == VotingModel.HASH;
    }

    public boolean acceptsVotes() {
        return votingModel.acceptsVotes();
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof VoteWeighting)) {
            return false;
        }
        VoteWeighting other = (VoteWeighting)o;
        return other.votingModel == this.votingModel
                && other.minBalanceModel == this.minBalanceModel
                && other.holdingId == this.holdingId
                && other.minBalance == this.minBalance;
    }

    @Override
    public int hashCode() {
        int hashCode = 17;
        hashCode = 31 * hashCode + Long.hashCode(holdingId);
        hashCode = 31 * hashCode + Long.hashCode(minBalance);
        hashCode = 31 * hashCode + minBalanceModel.hashCode();
        hashCode = 31 * hashCode + votingModel.hashCode();
        return hashCode;
    }

}
