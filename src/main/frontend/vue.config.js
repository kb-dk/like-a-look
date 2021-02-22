process.env.VUE_APP_VERSION =
  process.env.NODE_ENV === "production"
    ? require("./package.json").version
    : "DEVELOPMENT BUILD";
module.exports = {
  publicPath: process.env.NODE_ENV === 'production'
    ? '/like-a-look/'
    : '/'
};
