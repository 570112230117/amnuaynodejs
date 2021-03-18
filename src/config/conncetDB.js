var mysql = require("mysql");

var con = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "shopping",
});

con.connect(function (err) {
  if (err) throw err;
  con.query(
    "SELECT * FROM admin a join products p on p.id = a.id ",
    function (err, result, fields) {
      if (err) throw err;
      console.log(result);
    }
  );
});
