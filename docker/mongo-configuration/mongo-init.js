db.createUser(
    {
        user: "ernestochero",
        pwd: "1234#1234",
        roles: [
            {
                role: "readWrite",
                db: "hanamuradb"
            }
        ]
    }
);