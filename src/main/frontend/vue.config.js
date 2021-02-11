process.env.VUE_APP_VERSION =
  process.env.NODE_ENV === "production"
    ? require("./package.json").version
    : "DEVELOPMENT BUILD";
module.exports = {
  devServer: {
    proxy: {
      "^/api/upload/": {
        target: "http://labs.statsbiblioteket.dk/like-a-look/api/similar/",
        pathRewrite: { "^/api/upload/": "" },
        changeOrigin: true
      }
    }
  }
};
