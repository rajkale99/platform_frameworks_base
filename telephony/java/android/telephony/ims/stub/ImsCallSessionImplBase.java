/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.telephony.ims.stub;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsCallSessionListener;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsVideoCallProvider;
import android.telephony.ims.aidl.IImsCallSessionListener;

import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
/**
 * Base implementation of IImsCallSession, which implements stub versions of the methods available.
 *
 * Override the methods that your implementation of ImsCallSession supports.
 *
 * @hide
 */
@SystemApi
@TestApi
// DO NOT remove or change the existing APIs, only add new ones to this Base implementation or you
// will break other implementations of ImsCallSession maintained by other ImsServices.
public class ImsCallSessionImplBase implements AutoCloseable {
    /**
     * Notify USSD Mode.
     */
    public static final int USSD_MODE_NOTIFY = 0;
    /**
     * Request USSD Mode
     */
    public static final int USSD_MODE_REQUEST = 1;

    /**
     * Defines IMS call session state.
     */
    public static class State {
        public static final int IDLE = 0;
        public static final int INITIATED = 1;
        public static final int NEGOTIATING = 2;
        public static final int ESTABLISHING = 3;
        public static final int ESTABLISHED = 4;

        public static final int RENEGOTIATING = 5;
        public static final int REESTABLISHING = 6;

        public static final int TERMINATING = 7;
        public static final int TERMINATED = 8;

        public static final int INVALID = (-1);

        /**
         * Converts the state to string.
         */
        public static String toString(int state) {
            switch (state) {
                case IDLE:
                    return "IDLE";
                case INITIATED:
                    return "INITIATED";
                case NEGOTIATING:
                    return "NEGOTIATING";
                case ESTABLISHING:
                    return "ESTABLISHING";
                case ESTABLISHED:
                    return "ESTABLISHED";
                case RENEGOTIATING:
                    return "RENEGOTIATING";
                case REESTABLISHING:
                    return "REESTABLISHING";
                case TERMINATING:
                    return "TERMINATING";
                case TERMINATED:
                    return "TERMINATED";
                default:
                    return "UNKNOWN";
            }
        }

        /**
         * @hide
         */
        private State() {
        }
    }

    // Non-final for injection by tests
    private IImsCallSession mServiceImpl = new IImsCallSession.Stub() {
        @Override
        public void close() {
            ImsCallSessionImplBase.this.close();
        }

        @Override
        public String getCallId() {
            return ImsCallSessionImplBase.this.getCallId();
        }

        @Override
        public ImsCallProfile getCallProfile() {
            return ImsCallSessionImplBase.this.getCallProfile();
        }

        @Override
        public ImsCallProfile getLocalCallProfile() {
            return ImsCallSessionImplBase.this.getLocalCallProfile();
        }

        @Override
        public ImsCallProfile getRemoteCallProfile() {
            return ImsCallSessionImplBase.this.getRemoteCallProfile();
        }

        @Override
        public String getProperty(String name) {
            return ImsCallSessionImplBase.this.getProperty(name);
        }

        @Override
        public int getState() {
            return ImsCallSessionImplBase.this.getState();
        }

        @Override
        public boolean isInCall() {
            return ImsCallSessionImplBase.this.isInCall();
        }

        @Override
        public void setListener(IImsCallSessionListener listener) {
            ImsCallSessionImplBase.this.setListener(new ImsCallSessionListener(listener));
        }

        @Override
        public void setMute(boolean muted) {
            ImsCallSessionImplBase.this.setMute(muted);
        }

        @Override
        public void start(String callee, ImsCallProfile profile) {
            ImsCallSessionImplBase.this.start(callee, profile);
        }

        @Override
        public void startConference(String[] participants, ImsCallProfile profile) throws
                RemoteException {
            ImsCallSessionImplBase.this.startConference(participants, profile);
        }

        @Override
        public void accept(int callType, ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.accept(callType, profile);
        }

        @Override
        public void deflect(String deflectNumber) {
            ImsCallSessionImplBase.this.deflect(deflectNumber);
        }

        @Override
        public void reject(int reason) {
            ImsCallSessionImplBase.this.reject(reason);
        }

        @Override
        public void transfer(@NonNull String number, boolean isConfirmationRequired) {
            ImsCallSessionImplBase.this.transfer(number, isConfirmationRequired);
        }

        @Override
        public void consultativeTransfer(@NonNull IImsCallSession transferToSession) {
            ImsCallSessionImplBase otherSession = new ImsCallSessionImplBase();
            otherSession.setServiceImpl(transferToSession);
            ImsCallSessionImplBase.this.transfer(otherSession);
        }

        @Override
        public void terminate(int reason) {
            ImsCallSessionImplBase.this.terminate(reason);
        }

        @Override
        public void hold(ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.hold(profile);
        }

        @Override
        public void resume(ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.resume(profile);
        }

        @Override
        public void merge() {
            ImsCallSessionImplBase.this.merge();
        }

        @Override
        public void update(int callType, ImsStreamMediaProfile profile) {
            ImsCallSessionImplBase.this.update(callType, profile);
        }

        @Override
        public void extendToConference(String[] participants) {
            ImsCallSessionImplBase.this.extendToConference(participants);
        }

        @Override
        public void inviteParticipants(String[] participants) {
            ImsCallSessionImplBase.this.inviteParticipants(participants);
        }

        @Override
        public void removeParticipants(String[] participants) {
            ImsCallSessionImplBase.this.removeParticipants(participants);
        }

        @Override
        public void sendDtmf(char c, Message result) {
            ImsCallSessionImplBase.this.sendDtmf(c, result);
        }

        @Override
        public void startDtmf(char c) {
            ImsCallSessionImplBase.this.startDtmf(c);
        }

        @Override
        public void stopDtmf() {
            ImsCallSessionImplBase.this.stopDtmf();
        }

        @Override
        public void sendUssd(String ussdMessage) {
            ImsCallSessionImplBase.this.sendUssd(ussdMessage);
        }

        @Override
        public IImsVideoCallProvider getVideoCallProvider() {
            return ImsCallSessionImplBase.this.getVideoCallProvider();
        }

        @Override
        public boolean isMultiparty() {
            return ImsCallSessionImplBase.this.isMultiparty();
        }

        @Override
        public void sendRttModifyRequest(ImsCallProfile toProfile) {
            ImsCallSessionImplBase.this.sendRttModifyRequest(toProfile);
        }

        @Override
        public void sendRttModifyResponse(boolean status) {
            ImsCallSessionImplBase.this.sendRttModifyResponse(status);
        }

        @Override
        public void sendRttMessage(String rttMessage) {
            ImsCallSessionImplBase.this.sendRttMessage(rttMessage);
        }
    };

    /**
     * @hide
     */
    public final void setListener(IImsCallSessionListener listener) throws RemoteException {
        setListener(new ImsCallSessionListener(listener));
    }

    /**
     * Sets the listener to listen to the session events. An {@link ImsCallSession}
     * can only hold one listener at a time. Subsequent calls to this method
     * override the previous listener.
     *
     * @param listener {@link ImsCallSessionListener} used to notify the framework of updates
     * to the ImsCallSession
     */
    public void setListener(ImsCallSessionListener listener) {
    }

    /**
     * Closes the object. This {@link ImsCallSessionImplBase} is not usable after being closed.
     */
    @Override
    public void close() {

    }

    /**
     * @return A String containing the unique call ID of this {@link ImsCallSessionImplBase}.
     */
    public String getCallId() {
        return null;
    }

    /**
     * @return The {@link ImsCallProfile} that this {@link ImsCallSessionImplBase} is associated
     * with.
     */
    public ImsCallProfile getCallProfile() {
        return null;
    }

    /**
     * @return The local {@link ImsCallProfile} that this {@link ImsCallSessionImplBase} is
     * associated with.
     */
    public ImsCallProfile getLocalCallProfile() {
        return null;
    }

    /**
     * @return The remote {@link ImsCallProfile} that this {@link ImsCallSessionImplBase} is
     * associated with.
     */
    public ImsCallProfile getRemoteCallProfile() {
        return null;
    }

    /**
     * @param name The String extra key.
     * @return The string extra value associated with the specified property.
     */
    public String getProperty(String name) {
        return null;
    }

    /**
     * @return The {@link ImsCallSessionImplBase} state, defined in
     * {@link ImsCallSessionImplBase.State}.
     */
    public int getState() {
        return ImsCallSessionImplBase.State.INVALID;
    }

    /**
     * @return true if the {@link ImsCallSessionImplBase} is in a call, false otherwise.
     */
    public boolean isInCall() {
        return false;
    }

    /**
     * Mutes or unmutes the mic for the active call.
     *
     * @param muted true if the call should be muted, false otherwise.
     */
    public void setMute(boolean muted) {
    }

    /**
     * Initiates an IMS call with the specified number and call profile.
     * The session listener set in {@link #setListener(ImsCallSessionListener)} is called back upon
     * defined session events.
     * Only valid to call when the session state is in
     * {@link ImsCallSession.State#IDLE}.
     *
     * @param callee dialed string to make the call to
     * @param profile call profile to make the call with the specified service type,
     *      call type and media information
     * @see {@link ImsCallSession.Listener#callSessionStarted},
     * {@link ImsCallSession.Listener#callSessionStartFailed}
     */
    public void start(String callee, ImsCallProfile profile) {
    }

    /**
     * Initiates an IMS call with the specified participants and call profile.
     * The session listener set in {@link #setListener(ImsCallSessionListener)} is called back upon
     * defined session events.
     * The method is only valid to call when the session state is in
     * {@link ImsCallSession.State#IDLE}.
     *
     * @param participants participant list to initiate an IMS conference call
     * @param profile call profile to make the call with the specified service type,
     *      call type and media information
     * @see {@link ImsCallSession.Listener#callSessionStarted},
     * {@link ImsCallSession.Listener#callSessionStartFailed}
     */
    public void startConference(String[] participants, ImsCallProfile profile) {
    }

    /**
     * Accepts an incoming call or session update.
     *
     * @param callType call type specified in {@link ImsCallProfile} to be answered
     * @param profile stream media profile {@link ImsStreamMediaProfile} to be answered
     * @see {@link ImsCallSession.Listener#callSessionStarted}
     */
    public void accept(int callType, ImsStreamMediaProfile profile) {
    }

    /**
     * Deflects an incoming call.
     *
     * @param deflectNumber number to deflect the call
     */
    public void deflect(String deflectNumber) {
    }

    /**
     * Rejects an incoming call or session update.
     *
     * @param reason reason code to reject an incoming call, defined in {@link ImsReasonInfo}.
     *               The {@link android.telecom.InCallService} (dialer app) can use the
     *               {@link android.telecom.Call#reject(int)} API to reject a call while specifying
     *               a user-indicated reason for rejecting the call.
     *               Normal call declines ({@link android.telecom.Call#REJECT_REASON_DECLINED}) will
     *               map to {@link ImsReasonInfo#CODE_USER_DECLINE}.
     *               Unwanted calls ({@link android.telecom.Call#REJECT_REASON_UNWANTED}) will map
     *               to {@link ImsReasonInfo#CODE_SIP_USER_MARKED_UNWANTED}.
     * {@link ImsCallSession.Listener#callSessionStartFailed}
     */
    public void reject(int reason) {
    }

    /**
     * Transfer an established call to given number
     *
     * @param number number to transfer the call
     * @param isConfirmationRequired if {@code True}, indicates a confirmed transfer,
     * if {@code False} it indicates an unconfirmed transfer.
     * @hide
     */
    public void transfer(@NonNull String number, boolean isConfirmationRequired) {
    }

    /**
     * Transfer an established call to another call session
     *
     * @param otherSession The other ImsCallSession to transfer the ongoing session to.
     * @hide
     */
    public void transfer(@NonNull ImsCallSessionImplBase otherSession) {
    }

    /**
     * Terminates a call.
     *
     * @param reason reason code to terminate a call, defined in {@link ImsReasonInfo}.
     *
     * @see {@link ImsCallSession.Listener#callSessionTerminated}
     */
    public void terminate(int reason) {
    }

    /**
     * Puts a call on hold. When it succeeds, {@link ImsCallSession.Listener#callSessionHeld} is
     * called.
     *
     * @param profile stream media profile {@link ImsStreamMediaProfile} to hold the call
     * @see {@link ImsCallSession.Listener#callSessionHeld},
     * {@link ImsCallSession.Listener#callSessionHoldFailed}
     */
    public void hold(ImsStreamMediaProfile profile) {
    }

    /**
     * Continues a call that's on hold. When it succeeds,
     * {@link ImsCallSession.Listener#callSessionResumed} is called.
     *
     * @param profile stream media profile with {@link ImsStreamMediaProfile} to resume the call
     * @see {@link ImsCallSession.Listener#callSessionResumed},
     * {@link ImsCallSession.Listener#callSessionResumeFailed}
     */
    public void resume(ImsStreamMediaProfile profile) {
    }

    /**
     * Merges the active and held call. When the merge starts,
     * {@link ImsCallSession.Listener#callSessionMergeStarted} is called.
     * {@link ImsCallSession.Listener#callSessionMergeComplete} is called if the merge is
     * successful, and {@link ImsCallSession.Listener#callSessionMergeFailed} is called if the merge
     * fails.
     *
     * @see {@link ImsCallSession.Listener#callSessionMergeStarted},
     * {@link ImsCallSession.Listener#callSessionMergeComplete},
     *      {@link ImsCallSession.Listener#callSessionMergeFailed}
     */
    public void merge() {
    }

    /**
     * Updates the current call's properties (ex. call mode change: video upgrade / downgrade).
     *
     * @param callType call type specified in {@link ImsCallProfile} to be updated
     * @param profile stream media profile {@link ImsStreamMediaProfile} to be updated
     * @see {@link ImsCallSession.Listener#callSessionUpdated},
     * {@link ImsCallSession.Listener#callSessionUpdateFailed}
     */
    public void update(int callType, ImsStreamMediaProfile profile) {
    }

    /**
     * Extends this call to the conference call with the specified recipients.
     *
     * @param participants participant list to be invited to the conference call after extending the
     * call
     * @see {@link ImsCallSession.Listener#callSessionConferenceExtended},
     * {@link ImsCallSession.Listener#callSessionConferenceExtendFailed}
     */
    public void extendToConference(String[] participants) {
    }

    /**
     * Requests the conference server to invite an additional participants to the conference.
     *
     * @param participants participant list to be invited to the conference call
     * @see {@link ImsCallSession.Listener#callSessionInviteParticipantsRequestDelivered},
     *      {@link ImsCallSession.Listener#callSessionInviteParticipantsRequestFailed}
     */
    public void inviteParticipants(String[] participants) {
    }

    /**
     * Requests the conference server to remove the specified participants from the conference.
     *
     * @param participants participant list to be removed from the conference call
     * @see {@link ImsCallSession.Listener#callSessionRemoveParticipantsRequestDelivered},
     *      {@link ImsCallSession.Listener#callSessionRemoveParticipantsRequestFailed}
     */
    public void removeParticipants(String[] participants) {
    }

    /**
     * Sends a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2833</a>,
     * event 0 ~ 9 maps to decimal value 0 ~ 9, '*' to 10, '#' to 11, event 'A' ~ 'D' to 12 ~ 15,
     * and event flash to 16. Currently, event flash is not supported.
     *
     * @param c the DTMF to send. '0' ~ '9', 'A' ~ 'D', '*', '#' are valid inputs.
     * @param result If non-null, the {@link Message} to send when the operation is complete. This
     *         is done by using the associated {@link android.os.Messenger} in
     *         {@link Message#replyTo}. For example:
     * {@code
     *     // Send DTMF and other operations...
     *     try {
     *         // Notify framework that the DTMF was sent.
     *         Messenger dtmfMessenger = result.replyTo;
     *         if (dtmfMessenger != null) {
     *             dtmfMessenger.send(result);
     *         }
     *     } catch (RemoteException e) {
     *         // Remote side is dead
     *     }
     * }
     */
    public void sendDtmf(char c, Message result) {
    }

    /**
     * Start a DTMF code. According to <a href="http://tools.ietf.org/html/rfc2833">RFC 2833</a>,
     * event 0 ~ 9 maps to decimal value 0 ~ 9, '*' to 10, '#' to 11, event 'A' ~ 'D' to 12 ~ 15,
     * and event flash to 16. Currently, event flash is not supported.
     *
     * @param c the DTMF to send. '0' ~ '9', 'A' ~ 'D', '*', '#' are valid inputs.
     */
    public void startDtmf(char c) {
    }

    /**
     * Stop a DTMF code.
     */
    public void stopDtmf() {
    }

    /**
     * Sends an USSD message.
     *
     * @param ussdMessage USSD message to send
     */
    public void sendUssd(String ussdMessage) {
    }

    /**
     * See {@link #getImsVideoCallProvider()}, used directly in older ImsService implementations.
     * @hide
     */
    public IImsVideoCallProvider getVideoCallProvider() {
        ImsVideoCallProvider provider = getImsVideoCallProvider();
        return provider != null ? provider.getInterface() : null;
    }

    /**
     * @return The {@link ImsVideoCallProvider} implementation contained within the IMS service
     * process.
     */
    public ImsVideoCallProvider getImsVideoCallProvider() {
        return null;
    }

    /**
     * Determines if the current session is multiparty.
     * @return {@code True} if the session is multiparty.
     */
    public boolean isMultiparty() {
        return false;
    }

    /**
     * Device issues RTT modify request
     * @param toProfile The profile with requested changes made
     */
    public void sendRttModifyRequest(ImsCallProfile toProfile) {
    }

    /**
     * Device responds to Remote RTT modify request
     * @param status true if the the request was accepted or false of the request is defined.
     */
    public void sendRttModifyResponse(boolean status) {
    }

    /**
     * Device sends RTT message
     * @param rttMessage RTT message to be sent
     */
    public void sendRttMessage(String rttMessage) {
    }

    /** @hide */
    public IImsCallSession getServiceImpl() {
        return mServiceImpl;
    }

    /** @hide */
    public void setServiceImpl(IImsCallSession serviceImpl) {
        mServiceImpl = serviceImpl;
    }
}
