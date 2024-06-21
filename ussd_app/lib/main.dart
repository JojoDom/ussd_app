import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:logger/logger.dart';


void main() {
  runApp(const MaterialApp(home: MyUSSDService(),));
}

class MyUSSDService extends StatefulWidget {
  const MyUSSDService({super.key});

  @override
  State<MyUSSDService> createState() => _MyUSSDServiceState();
}

class _MyUSSDServiceState extends State<MyUSSDService> {
  static const platform = MethodChannel('groceries.ussd');
  TextEditingController controller = TextEditingController();
  bool isLoading = false;
  bool isLoadingMulti = false;
  String ussdResponse = '';
  List<USSDSession> sessions = [];
  String multiSessionResponse = '';


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('USSD Test')),
      body: Container(
        margin: const EdgeInsets.symmetric(vertical: 20, horizontal: 30),
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 20),
              child: TextFormField(
                controller: controller,
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 30),
              child: ElevatedButton(
                onPressed: isLoading ? null : () async {
                  Logger().i(controller.text);
                  setState(() {
                    isLoading = true;
                    ussdResponse = '';
                  });
                  try {
                    final result = await platform.invokeMethod('sendUssd', {'ussdCode': controller.text});
                    setState(() {
                      ussdResponse = result;
                      isLoading = false;
                    });
                  } on PlatformException catch (e) {
                    setState(() {
                      ussdResponse = e.message ?? 'Unknown error occurred';
                      isLoading = false;
                    });
                  }
                },
                child: isLoading 
                  ? const CircularProgressIndicator()
                  : const Text('Single Session', style: TextStyle(color: Colors.black)),
              ),
            ),
            
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 30),
              child: ElevatedButton(
                onPressed: isLoading ? null : () async {
                  Logger().i(controller.text);
                  setState(() {
                    isLoading = true;
                    multiSessionResponse = '';
                  });
                  try {
                    final result = await platform.invokeMethod('sendUssd', {'ussdCode': controller.text});
                    setState(() {
                      multiSessionResponse = result;
                      isLoading = false;
                      sessions.add(USSDSession(controller.text, result));
                    });
                  } on PlatformException catch (e) {
                    setState(() {
                      multiSessionResponse = e.message ?? 'Unknown error occurred';
                      isLoading = false;
                    });
                  }
                },
                child: isLoading 
                  ? const CircularProgressIndicator()
                  : const Text('Multisession', style: TextStyle(color: Colors.black)),
              ),
            ),
            if (ussdResponse.isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Text(ussdResponse, style: const TextStyle(color: Colors.black)),
              ),
          Expanded(
              child: ListView.builder(
                itemCount: sessions.length,
                itemBuilder: (context, index) {
                  return ListTile(
                    title: Text(sessions[index].code),
                    subtitle: Text(sessions[index].response),
                  );
                },
              ),
            )
          ],
        ),
      ),
    );
  }
}




class USSDSession {
  final String code;
  final String response;

  USSDSession(this.code, this.response);
}