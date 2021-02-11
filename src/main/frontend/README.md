# like-a-look test frontend

The test frontend detects faces using the webcam and calls the like-a-look
API to find similar faces. Note that this only works as a tech demo when used 
out of the box as like-a-look does not come pre-packaged with faces.

## Absolute frontend beginner

The test frontend is a Vue app.

The files in the `src/main/frontend/` contains the files needed for generating
the static front end files, which are placed in the `src/main/webapp/` folder.

The files is `src/main/webapp/` are part of the like-a-look repo. 
They only need to be (re)generated if the frontend is changed.

The base prerequisite for building is `npm`.<br>
Install with `sudo apt install npm` or similar.


## Project setup
```shell
npm install
```

### Compiles and hot-reloads for development
```shell
npm run serve
```

### Compiles and minifies for production
```shell
npm run build
```

### Lints and fixes files
```shell
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).


### Copy changes to the repo

```shell
cp -r dist/* ../webapp/
```