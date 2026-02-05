const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendEmergencyNotification = functions.firestore
    .document('ai_triage_logs/{docId}')
    .onCreate((snapshot, context) => {
        const data = snapshot.data();

        if (data.priority === 'High Risk') {
            const payload = {
                notification: {
                    title: '🚨 CRITICAL EMERGENCY',
                    body: `High Risk Patient: ${data.patientAge}y/${data.patientGender}. Symptoms: ${data.symptoms}`,
                    clickAction: 'FLUTTER_NOTIFICATION_CLICK' // or Android intent
                },
                data: {
                    caseId: context.params.docId,
                    type: 'emergency'
                }
            };

            return admin.messaging().sendToTopic('emergency_alerts', payload);
        }
        return null;
    });

exports.checkStockLevels = functions.firestore
    .document('medicine_inventory/{docId}')
    .onUpdate((change, context) => {
        const data = change.after.data();
        if (data.stock < 10) {
            // Alert Pharmacy
            const payload = {
                notification: {
                    title: 'Low Stock Alert',
                    body: `${data.name} is running low (${data.stock} remaining).`
                }
            };
            return admin.messaging().sendToTopic('pharmacy_alerts', payload);
        }
        return null;
    });
