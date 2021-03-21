#!/usr/bin/env wolframscript
(*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *)
begin = Now;

(* CONFIGURATION *)

featuresFile = "extracted-features-2021-03-21T20.04.48.mx";

(* Default domain and port *)
domain="127.0.0.1";
port  ="5858";

(* END OF CONFIGURATION *)

(* BEGIN PROGRAM *)
logString[s_String] := Module[
  {len = StringLength@s},
  "##"
  <> StringRepeat[" ", 38 - Floor[len/2]]
  <> s
  <> StringRepeat[" ", 38 - Ceiling[len/2]]
  <> "##"
]

Print[StringRepeat["#",80]]
Print[logString@""]
Print[logString@"** BEGIN PROGRAM: FACE SEARCH WEB SERVER **"]
Print[logString[DateString[]]]
Print[logString@""]
Print[logString@"CONFIGURATION"]
Print[logString@""]
Print["## Load features from "<>featuresFile<>"."]

(* Modify default domain and port if values given on command line *)
Switch[Length@$ScriptCommandLine,
    3, domain = $ScriptCommandLine[[2]]; port = $ScriptCommandLine[[3]],
    2, port = $ScriptCommandLine[[2]]]

Print["## Domain:     ", domain];
Print["## Port:       ", port];
Print["## $ProcessID: ", $ProcessID];

Print[logString@""]
Print[StringRepeat["#",80]]
(* END OF CONFIGURATION *)

ApacheDateString[] := With[{tz = StringReplace[DateString["ISOTimeZone"], ":" -> ""]},
  DateString[{"[", "Day", "/", "Month", "/", "Year", ":", "Hour", ":", "Minute", ":", "Second", " ", tz, "]"}]];

ApacheLog[ str_String ] := Module[{}, Print[ domain<>" - - " <> ApacheDateString[] <> " \"POST / HTTP/1.1\" 200 " <> StringTrim[str] ] ];

Print["## Load and initialize ANN data"];
Get@featuresFile;
features = faceFeatures[[All,2]];
imagePaths = faceFeatures[[All,1]];
Print["## Loaded 2048 dimensional features from "<>ToString@Length@features<>" images."];

featureExtractor = NetModel["ResNet-101 Trained on Augmented CASIA-WebFace Data"];
findNearestPhoto = FeatureNearest[features -> {"Index", "Distance"}];
Print["## Created a findNearest function giving index and distance"]
Print["## All data loaded and initialized"];

resultJSON[testImagePath_String, n_Integer] := Module[
    {image, faces, transformedFace, nearestFaces},

    Catch[image = Import[testImagePath, "PNG"]];
    Print["## Image loaded: "<>ToString@Head@image];

    If[Not[Head@image === Image], Print[logString@"!! ERROR !!"]; Print["## Unable to process image"];ExportString[<|"error"->"Unable to import PNG file."|>,"JSON"],

	Print["## Look for faces"];
        faces = FaceAlign[image, Automatic, {224,224}];
	Print["## Found "<>ToString@Length@faces<>" faces in image"];
        If[Length @ faces == 0, ExportString[<| "error" -> "Found no face" |>, "JSON"],

	   Print["## Extract features from the B/W version of the first found face, and find the nearest "<>ToString@n<>" faces."];
           nearestFaces = findNearestPhoto[featureExtractor@ColorConvert[First@faces, "Grayscale"], n];

	   Print["## Return a JSON representation"];
           ExportString[
               <|
                   "portraits" -> 
                      Map[
                           <|"id" -> FileBaseName@FileNameTake@imagePaths[[#[[1]]]], 
                             "distance" -> #[[2]]
                            |> &, 
                          nearestFaces
                      ]
               |>, "JSON"]
      ]
   ]
]

listener = SocketListen[
  SocketOpen[domain<>":"<>port],
  Function[{assoc},
    With[{
      client = assoc["SourceSocket"],
      data = assoc["Data"]
    },
    request = ImportString[data, "HTTPRequest"];
    origin = Association[ request["Headers"] ]["origin"];
    If[ Head[origin]===Missing, origin="" ];

    begin = Now;

    parameters = Association@request["Query"];
    Print["## Recieved request: "<>ToString@parameters];

    result = ExportString[<|"error"->"No data produced"|>,"JSON"];
    If[Not[KeyExistsQ[parameters,"imageurl"]], Print["## ERROR: No URL parameter"],
            imageUrl = parameters["imageurl"];
            result = resultJSON[imageUrl,3];
    ];

    response = ExportString[
         HTTPResponse[ result, 
   <|
        "StatusCode" -> 200,
        "ContentType" -> "application/json",
        "Headers" -> { "Access-Control-Allow-Origin" -> origin }
         |>
      ], 
      "HTTPResponse"
    ];

    WriteString[client, response];
    Close[client]
    Print["## Request processed in "<>ToString[Now-begin]]
    ]
   ]
  ];

url = URLBuild[<|"Scheme" -> "http", "Domain" -> First[listener["Socket"]["DestinationIPAddress"]], "Port" -> listener["Socket"]["DestinationPort"]|>];

Print["Listening:  ", url, "\n"];

task = ZeroMQLink`Private`$AsyncState["Task"];
WaitAsynchronousTask[task];
Print["Exiting..."];
