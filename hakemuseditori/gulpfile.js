var gulp = require('gulp'),
    browserify = require('gulp-browserify'),
    concat = require('gulp-concat'),
    less = require('gulp-less'),
    jshint = require('gulp-jshint'),
    livereload = require('gulp-livereload'),
    templates = require('gulp-angular-templatecache'),
    uglify = require('gulp-uglify'),
    gulpif = require('gulp-if'),
    ngAnnotate = require('gulp-ng-annotate')

var jsFiles = 'src/main/js/**/*.js';
var isWatch

function handleError(err) {
    console.log(err.toString());
    this.emit('end');
    if (!isWatch) {
      throw err
    }
}

gulp.task('lint', function() {
    gulp.src(jsFiles)
        .pipe(jshint({
            globals: {
                require: false,
                angular: false
            }
        }))
        .pipe(jshint.reporter('default'));
});

gulp.task("templates", function() {
  return gulp.src("src/main/templates/**/*.html")
    .pipe(templates("hakemuseditori-templates.js", { root:"templates/"}))
    .pipe(gulp.dest("dist"))
})

gulp.task('less', function () {
  gulp.src('src/main/less/**/main.less')
    .pipe(less().on('error', handleError))
    .pipe(concat('main.css'))
    .pipe(gulp.dest('src/main/webapp/css'));
  gulp.src('src/main/less/**/preview.less')
    .pipe(less().on('error', handleError))
    .pipe(concat('preview.css'))
    .pipe(gulp.dest('src/main/webapp/css'));
});

gulp.task('browserify', ["templates"], function() {
  compileJs(false)
})

gulp.task('browserify-min', ["templates"], function() {
  compileJs(true)
})

function compileJs(compress) {
  gulp.src(['src/main/js/hakemuseditori.js'])
    .pipe(browserify({
      insertGlobals: true,
      debug: true,
      require: "./hakemuseditori"
    }).on('error', handleError))
    .pipe(concat('hakemuseditori.js'))
    .pipe(gulpif(compress, ngAnnotate()))
    .pipe(gulpif(compress, uglify({ mangle: true })))
    .pipe(gulp.dest('dist'))
}

gulp.task('watch', function() {
    isWatch = true
    livereload.listen();
    gulp.watch(['src/main/webapp/**/*.js', 'src/main/webapp/**/*.css', 'src/main/webapp/**/*.html'], livereload.changed);
    gulp.watch(['src/main/templates/**/*.html'], ['compile-dev'])
    gulp.watch([jsFiles],['lint', 'browserify']);
    gulp.watch(['src/main/less/**/*.less'],['less']);
});

gulp.task('compile', ['templates', 'browserify-min', 'less']);
gulp.task('compile-dev', ['templates', 'browserify', 'less']);
gulp.task('dev', ['lint', 'compile-dev', 'watch']);
gulp.task('default', ['dev']);