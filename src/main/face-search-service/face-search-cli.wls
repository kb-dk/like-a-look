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

(* Features of all photos: featuresOfAllPhotos *)
featuresFile = "extracted-features-2021-03-21T20.04.48.mx";

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
Print[logString@"** BEGIN PROGRAM: SEARCH FOR SIMILAR FACES **"]
Print[logString[DateString[]]]
Print[logString@""]
Print[logString@"CONFIGURATION"]
Print[logString@""]
Print["## Load features from "<>featuresFile<>"."]

If[Length@$ScriptCommandLine != 2,
  Print[logString@"!!! ERROR !!!"]
  Print["## Usage: similar-face-search.wls <path-to-input-image>"]
  Print["## You need to provide a path to an input image"];Exit[]]

inputImagePath = $ScriptCommandLine[[2]];
Print["## SEARCH FOR FACES SIMILAR TO "<>inputImagePath]

Print[logString@""]
Print[StringRepeat["#",80]]

Print@"## Load ResNet-101 Trained on Augmented CASIA-WebFace Data."
EchoTiming[featureExtractor = NetModel["ResNet-101 Trained on Augmented CASIA-WebFace Data"]]

inputImage = Import[inputImagePath, "PNG"];

If[Not[Head@inputImage === Image],
  Print[logString@"!!! ERROR !!!"]
  Print["## Unable to load input PNG image "<>ToString@$MessageList]
  Exit[]]
Print["## Input image loaded"]

Print["## Extract face from input image."]
inputFace = EchoTiming[FaceAlign[inputImage, Automatic, {224,224}]]
Print["## Found "<>ToString@Length@inputFace<>" faces in input image."]

If[Length@inputFace == 0,
  Print[logString@"!!! ERROR !!!"]
  Print["## No face found in image "<>inputImagePath]
  Exit[]]

Print@"## Convert input face to B/W and extract the features of the first face found."
inputFaceFeatures = EchoTiming[featureExtractor@ColorConvert[First@inputFace, "Grayscale"]];

Print@"## Load the global features into 'faceFeatures'"
EchoTiming[Get@featuresFile];
filePaths= faceFeatures[[All,1]];
features = faceFeatures[[All,2]];
Print["## Loaded features from "<>ToString@Length@faceFeatures<>" faces"]

Print"## Setup the search function to return the index and distance of search results."
findNearestFaces = FeatureNearest[features -> {"Index", "Distance"}];

Print[StringRepeat["#",80]]
Print[logString@"** RESULT **"]
Print[ExportString[
  <|
    "portraits" -> 
      Map[
        <|"id" -> 
          FileBaseName@FileNameTake@filePaths[[#[[1]]]], 
          "distance" -> #[[2]]
        |> &, 
        findNearestFaces[inputFaceFeatures, 3]
      ]
  |>,
  "JSON"
]]
Print[StringRepeat["#",80]]

Print[StringRepeat["#",80]]
Print[logString@""]
Print[logString["RUN TIME: "<>ToString[Now-begin]]]
Print[logString["SEARCHED "<>ToString@Length@features<>" FACES."]]
Print[logString["MEMORY IN USE: "<>ToString@NumberForm[MemoryInUse[], DigitBlock -> 3, NumberSeparator -> " "]<>" B."]]
Print[logString@""]
Print[logString[DateString[]]]
Print[logString@"** END OF PROGRAM **"]
Print[logString@""]
Print[StringRepeat["#",80]]
